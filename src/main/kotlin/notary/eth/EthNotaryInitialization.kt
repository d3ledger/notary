package notary.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.EthereumPasswords
import io.reactivex.Observable
import jp.co.soramitsu.iroha.Keypair
import mu.KLogging
import notary.Notary
import notary.NotaryConfig
import notary.createEthNotary
import notary.endpoint.RefundServerEndpoint
import notary.endpoint.ServerInitializationBundle
import notary.endpoint.eth.EthRefundStrategyImpl
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import provider.eth.EthRelayProvider
import provider.eth.EthTokensProvider
import sidechain.SideChainEvent
import sidechain.eth.EthChainHandler
import sidechain.eth.EthChainListener
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.consumer.IrohaNetworkImpl
import java.math.BigInteger

/**
 * Class for notary instantiation
 * @param ethRelayProvider - provides with white list of ethereum wallets
 * @param ethTokensProvider - provides with white list of ethereum ERC20 tokens
 */
class EthNotaryInitialization(
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
        logger.info { "Eth notary initialization" }
        return initEthChain()
            .map { ethEvent ->
                initNotary(ethEvent)
            }
            .flatMap { it.initIrohaConsumer() }
            .map { initRefund() }
    }

    /**
     * Init Ethereum chain listener
     * @return Observable on Ethereum sidechain events
     */
    private fun initEthChain(): Result<Observable<SideChainEvent.PrimaryBlockChainEvent>, Exception> {
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
        ethEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>
    ): Notary {
        logger.info { "Init Notary notary" }
        return createEthNotary(notaryConfig, ethEvents)
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
                irohaKeyPair
            )
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
