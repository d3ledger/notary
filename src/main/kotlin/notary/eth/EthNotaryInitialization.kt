package notary.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.EthereumPasswords
import io.reactivex.Observable
import model.IrohaCredential
import mu.KLogging
import notary.Notary
import notary.createEthNotary
import notary.endpoint.RefundServerEndpoint
import notary.endpoint.ServerInitializationBundle
import notary.endpoint.eth.EthRefundStrategyImpl
import okhttp3.OkHttpClient
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import provider.NotaryPeerListProviderImpl
import provider.eth.EthRelayProvider
import provider.eth.EthTokensProvider
import sidechain.SideChainEvent
import sidechain.eth.EthChainHandler
import sidechain.eth.EthChainListener
import sidechain.eth.util.BasicAuthenticator
import sidechain.iroha.consumer.IrohaNetwork
import java.math.BigInteger

/**
 * Class for notary instantiation
 * @param ethRelayProvider - provides with white list of ethereum wallets
 * @param ethTokensProvider - provides with white list of ethereum ERC20 tokens
 */
class EthNotaryInitialization(
    private val notaryCredential: IrohaCredential,
    private val irohaNetwork: IrohaNetwork,
    private val ethNotaryConfig: EthNotaryConfig,
    private val passwordsConfig: EthereumPasswords,
    private val ethRelayProvider: EthRelayProvider,
    private val ethTokensProvider: EthTokensProvider
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
            .flatMap { notary -> notary.initIrohaConsumer() }
            .map { initRefund() }
    }

    /**
     * Init Ethereum chain listener
     * @return Observable on Ethereum sidechain events
     */
    private fun initEthChain(): Result<Observable<SideChainEvent.PrimaryBlockChainEvent>, Exception> {
        logger.info { "Init Eth chain" }

        val builder = OkHttpClient().newBuilder()
        builder.authenticator(BasicAuthenticator(passwordsConfig))
        val web3 = Web3j.build(HttpService(ethNotaryConfig.ethereum.url, builder.build(), false))

        /** List of all observable wallets */
        val ethHandler = EthChainHandler(web3, ethRelayProvider, ethTokensProvider)
        return EthChainListener(
            web3,
            BigInteger.valueOf(ethNotaryConfig.ethereum.confirmationPeriod)
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
        logger.info { "Init ethereum Notary notary" }

        val peerListProvider = NotaryPeerListProviderImpl(
            notaryCredential,
            irohaNetwork,
            ethNotaryConfig.notaryListStorageAccount,
            ethNotaryConfig.notaryListSetterAccount
        )

        return createEthNotary(notaryCredential, irohaNetwork, ethEvents, peerListProvider)
    }

    /**
     * Init refund notary.endpoint
     */
    private fun initRefund() {
        logger.info { "Init Refund endpoint" }
        RefundServerEndpoint(
            ServerInitializationBundle(ethNotaryConfig.refund.port, ethNotaryConfig.refund.endpointEthereum),
            EthRefundStrategyImpl(
                ethNotaryConfig,
                irohaNetwork,
                notaryCredential,
                ethNotaryConfig.ethereum,
                passwordsConfig,
                ethTokensProvider
            )
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
