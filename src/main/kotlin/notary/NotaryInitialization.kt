package notary

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.EthereumPasswords
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import jp.co.soramitsu.iroha.Keypair
import mu.KLogging
import notary.endpoint.RefundServerEndpoint
import notary.endpoint.ServerInitializationBundle
import notary.endpoint.eth.EthRefundStrategyImpl
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import provider.EthRelayProvider
import provider.EthTokensProvider
import sidechain.SideChainEvent
import sidechain.eth.EthChainHandler
import sidechain.eth.EthChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import java.math.BigInteger

/**
 * Class for notary instantiation
 * @param ethRelayProvider - provides with white list of ethereum wallets
 * @param ethTokensProvider - provides with white list of ethereum ERC20 tokens
 */
class NotaryInitialization(
    private val irohaKeyPair: Keypair,
    private val notaryConfig: NotaryConfig,
    private val passwordsConfig: EthereumPasswords,
    private val ethRelayProvider: EthRelayProvider,
    private val ethTokensProvider: EthTokensProvider,
    private val irohaNetwork: IrohaNetwork = IrohaNetworkImpl(notaryConfig.iroha.hostname, notaryConfig.iroha.port)
) {
    /**
     * Init notary
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Notary initialization" }
        return initEthChain()
            .map { ethEvent ->
                initNotary(ethEvent)
            }
            .flatMap { initIrohaConsumer(it) }
            .map { initRefund() }

    }

    /**
     * Init Ethereum chain listener
     * @return Observable on Ethereum sidechain events
     */
    private fun initEthChain(): Result<Observable<SideChainEvent.EthereumEvent>, Exception> {
        logger.info { "Init Eth chain" }

        val web3 = Web3j.build(HttpService(notaryConfig.ethereum.url))
        /** List of all observable wallets */
        val ethHandler = EthChainHandler(web3, ethRelayProvider, ethTokensProvider)
        return EthChainListener(
            web3,
            BigInteger.valueOf(notaryConfig.ethereum.confirmationPeriod)
        ).getBlockObservable()
            .map { observable ->
                observable.flatMapIterable { ethHandler.parseBlock(it) }
            }
    }

    /**
     * Init Notary
     */
    private fun initNotary(
        ethEvents: Observable<SideChainEvent.EthereumEvent>
    ): Notary {
        logger.info { "Init Notary notary" }
        return NotaryImpl(notaryConfig, ethEvents)
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
                        {
                            irohaConsumer.sendAndCheck(it)
                                .fold(
                                    { logger.info { "send to Iroha success" } },
                                    { logger.error { "send failure $it" } }
                                )
                        },
                        // on error
                        { logger.error { "OnError called $it" } },
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
            EthRefundStrategyImpl(
                notaryConfig.iroha,
                irohaNetwork,
                notaryConfig.ethereum,
                passwordsConfig,
                irohaKeyPair,
                notaryConfig.whitelistSetter
            )
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
