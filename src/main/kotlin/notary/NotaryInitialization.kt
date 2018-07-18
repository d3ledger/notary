package notary

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import mu.KLogging
import notary.endpoint.RefundServerEndpoint
import notary.endpoint.ServerInitializationBundle
import notary.endpoint.eth.EthRefundStrategyImpl
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import sidechain.SideChainEvent
import sidechain.eth.EthChainHandler
import sidechain.eth.EthChainListener
import sidechain.iroha.IrohaChainHandler
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import java.math.BigInteger

/**
 * Class for notary instantiation
 * @param ethWalletsProvider - provides with white list of ethereum wallets
 * @param ethTokensProvider - provides with white list of ethereum ERC20 tokens
 */
class NotaryInitialization(
    val notaryConfig: NotaryConfig,
    val ethWalletsProvider: EthWalletsProvider,
    val ethTokensProvider: EthTokensProvider = EthTokensProviderImpl(notaryConfig.db),
    val irohaNetwork: IrohaNetwork = IrohaNetworkImpl(notaryConfig.iroha.hostname, notaryConfig.iroha.port)
) {
    val irohaAccount = notaryConfig.iroha.creator
    val irohaKeypair =
        ModelUtil.loadKeypair(notaryConfig.iroha.pubkeyPath, notaryConfig.iroha.privkeyPath).get()

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

        val web3 = Web3j.build(HttpService(notaryConfig.ethereum.url))
        /** List of all observable wallets */
        return ethWalletsProvider.getWallets()
            .fanout {
                /** List of all observable ERC20 tokens */
                ethTokensProvider.getTokens()
            }.flatMap { (wallets, tokens) ->
                val ethHandler = EthChainHandler(web3, wallets, tokens)
                EthChainListener(
                    web3,
                    BigInteger.valueOf(notaryConfig.ethereum.confirmationPeriod)
                ).getBlockObservable()
                    .map { observable ->
                        observable.flatMapIterable { ethHandler.parseBlock(it) }
                    }
            }
    }

    /**
     * Init Iroha chain listener
     * @return Observable on Iroha sidechain events
     */
    private fun initIrohaChain(): Result<Observable<SideChainEvent.IrohaEvent>, Exception> {
        logger.info { "Init Iroha chain" }
        return IrohaChainListener(
            notaryConfig.iroha.hostname,
            notaryConfig.iroha.port,
            irohaAccount, irohaKeypair
        ).getBlockObservable()
            .map { observable ->
                observable.flatMapIterable { IrohaChainHandler().parseBlock(it) }
            }
    }

    /**
     * Init Notary
     */
    private fun initNotary(
        ethEvents: Observable<SideChainEvent>,
        irohaEvents: Observable<SideChainEvent.IrohaEvent>
    ): Notary {
        logger.info { "Init Notary notary" }
        return NotaryImpl(notaryConfig, ethEvents, irohaEvents)
    }

    /**
     * Init Iroha consumer
     */
    private fun initIrohaConsumer(notary: Notary): Result<Unit, Exception> {
        logger.info { "Init Iroha consumer" }
        return ModelUtil.loadKeypair(notaryConfig.iroha.pubkeyPath, notaryConfig.iroha.privkeyPath)
            .map {
                val irohaConsumer = IrohaConsumerImpl(notaryConfig.iroha)

                // Init Iroha Consumer pipeline
                notary.irohaOutput()
                    // convert from Notary model to Iroha model
                    // TODO rework Iroha batch transaction
                    .flatMapIterable { IrohaConverterImpl().convert(it) }
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                        // send to Iroha network layer
                        { irohaConsumer.sendAndCheck(it) },
                        // on error
                        { logger.error { it } },
                        // should be never called
                        { logger.error { "OnComplete called" } }
                    )
                Unit
            }
    }

    /**
     * Init refund notary.endpoint
     */
    private fun initRefund() {
        logger.info { "Init Refund notary.endpoint" }
        RefundServerEndpoint(
            ServerInitializationBundle(notaryConfig.refund.port, notaryConfig.refund.endpointEthereum),
            EthRefundStrategyImpl(notaryConfig.iroha, irohaNetwork, notaryConfig.ethereum, irohaKeypair)
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
