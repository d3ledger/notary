package notary

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import endpoint.RefundServerEndpoint
import endpoint.ServerInitializationBundle
import endpoint.eth.EthRefundStrategyImpl
import io.reactivex.Observable
import jp.co.soramitsu.iroha.Hash
import main.ConfigKeys
import mu.KLogging
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import sidechain.SideChainEvent
import sidechain.eth.EthChainHandler
import sidechain.eth.EthChainListener
import sidechain.iroha.IrohaChainHandler
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.consumer.IrohaKeyLoader
import sidechain.iroha.consumer.IrohaNetworkImpl


/**
 * Class for notary instantiation
 * @param ethWalletsProvider - provides with white list of ethereum wallets
 * @param ethTokensProvider - provides with white list of ethereum ERC20 tokens
 */
class NotaryInitialization(
    val ethWalletsProvider: EthWalletsProvider = EthWalletsProviderImpl(),
    val ethTokensProvider: EthTokensProvider = EthTokensProviderImpl()
) {

    /**
     * Init notary
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Notary initialization" }
        return initEthChain()
            .fanout { initIrohaChain() }
            .map { (ethEvent, irohaEvents) ->
                initNotary(ethEvent, irohaEvents)
            }
            .flatMap { initIrohaConsumer(it) }
            .map { initRefund() }
    }

    /**
     * Init Ethereum chain listener
     * @return Observable on Ethereum sidechain events
     */
    private fun initEthChain(): Result<Observable<SideChainEvent>, Exception> {
        logger.info { "Init Eth chain" }

        val web3 = Web3j.build(HttpService(CONFIG[ConfigKeys.notaryEthConnectionUrl]))
        /** List of all observable wallets */
        return ethWalletsProvider.getWallets()
            .fanout {
                /** List of all observable ERC20 tokens */
                ethTokensProvider.getTokens()
            }.flatMap { (wallets, tokens) ->
                val ethHandler = EthChainHandler(web3, wallets, tokens)
                EthChainListener(web3).getBlockObservable()
                    .map { observable ->
                        observable.flatMapIterable { ethHandler.parseBlock(it) }
                    }
            }
    }

    /**
     * Init Iroha chain listener
     * @return Observable on Iroha sidechain events
     */
    private fun initIrohaChain(): Result<Observable<SideChainEvent>, Exception> {
        logger.info { "Init Iroha chain" }
        return IrohaChainListener().getBlockObservable()
            .map { observable ->
                observable.flatMapIterable { IrohaChainHandler().parseBlock(it) }
            }
    }

    /**
     * Init Notary
     */
    private fun initNotary(ethEvents: Observable<SideChainEvent>, irohaEvents: Observable<SideChainEvent>): Notary {
        logger.info { "Init Notary notary" }
        return NotaryImpl(ethEvents, irohaEvents)
    }

    /**
     * Init Iroha consumer
     */
    private fun initIrohaConsumer(notary: Notary): Result<Unit, Exception> {
        logger.info { "Init Iroha consumer" }
        return IrohaKeyLoader.loadKeypair(CONFIG[ConfigKeys.notaryPubkeyPath], CONFIG[ConfigKeys.notaryPrivkeyPath])
            .map {
                val irohaConsumer = IrohaConsumerImpl(it)

                lateinit var hash: Hash
                // Init Iroha Consumer pipeline
                notary.irohaOutput()
                    // convert from Notary model to Iroha model
                    // TODO rework Iroha batch transaction
                    .flatMapIterable { IrohaConverterImpl().convert(it) }
                    // convert from Iroha model to Protobuf representation
                    .map {
                        hash = it.hash()
                        irohaConsumer.convertToProto(it)
                    }
                    .subscribe(
                        // send to Iroha network layer
                        {
                            IrohaNetworkImpl(
                                CONFIG[ConfigKeys.notaryIrohaHostname],
                                CONFIG[ConfigKeys.notaryIrohaPort]
                            ).sendAndCheck(it, hash)
                        },
                        // on error
                        { logger.error { it } }
                    )
                Unit
            }
    }

    /**
     * Init refund endpoint
     */
    private fun initRefund() {
        logger.info { "Init Refund endpoint" }
        val keys = IrohaKeyLoader.loadKeypair(CONFIG[ConfigKeys.notaryPubkeyPath], CONFIG[ConfigKeys.notaryPrivkeyPath])
        RefundServerEndpoint(
            ServerInitializationBundle(CONFIG[ConfigKeys.notaryRefundPort], CONFIG[ConfigKeys.notaryEthEndpoint]),
            EthRefundStrategyImpl(keys.get())
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
