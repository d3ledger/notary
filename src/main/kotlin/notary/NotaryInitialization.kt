package notary

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import endpoint.RefundServerEndpoint
import endpoint.ServerInitializationBundle
import endpoint.eth.EthNotaryResponse
import endpoint.eth.EthRefund
import endpoint.eth.EthRefundRequest
import io.reactivex.Observable
import main.CONFIG
import main.ConfigKeys
import mu.KLogging
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import sideChain.eth.EthChainHandler
import sideChain.eth.EthChainListener
import sideChain.iroha.IrohaChainHandler
import sideChain.iroha.IrohaChainListener
import sideChain.iroha.consumer.*
import java.math.BigInteger

/**
 * Class for notary instantiation
 * @param ethWalletsProvider - provides with white list of ethereum wallets
 * @param ethTokensProvider - provides with white list of ethereum ERC20 tokens
 */
class NotaryInitialization(
    val ethWalletsProvider: EthWalletsProvider = EthWalletsProviderImpl(),
    val ethTokensProvider: EthTokensProvider = EthTokensProviderImpl()
) {

    private lateinit var refundServerEndpoint: RefundServerEndpoint

    private val web3 = Web3j.build(HttpService(CONFIG[ConfigKeys.ethConnectionUrl]))
    private val ethChainListener = EthChainListener(web3)

    // ------------------------------------------| Iroha |------------------------------------------
    private var irohaChainListener = IrohaChainListener()
    private val irohaHandler = IrohaChainHandler()


    private val irohaConverter = IrohaConverterImpl()
    private val irohaNetwork = IrohaNetworkImpl()
    private lateinit var irohaConsumer: IrohaConsumer

    // ------------------------------------------| Notary |------------------------------------------
    private lateinit var notary: Notary

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
            .flatMap { initIrohaConsumer() }
            .map { initRefund() }
    }

    /**
     * Init Ethereum chain listener
     * @return Observable on Ethereum sidechain events
     */
    private fun initEthChain(): Result<Observable<NotaryInputEvent>, Exception> {
        logger.info { "Init Eth chain" }

        /** List of all observable wallets */
        return ethWalletsProvider.getWallets()
            .fanout {
                /** List of all observable ERC20 tokens */
                ethTokensProvider.getTokens()
            }.flatMap { (wallets, tokens) ->
                val ethHandler = EthChainHandler(web3, wallets, tokens)
                ethChainListener.getBlockObservable()
                    .map { observable ->
                        observable.flatMapIterable { ethHandler.parseBlock(it) }
                    }
            }
    }

    /**
     * Init Iroha chain listener
     * @return Observable on Iroha sidechain events
     */
    private fun initIrohaChain(): Result<Observable<NotaryInputEvent>, Exception> {
        logger.info { "Init Iroha chain" }
        return irohaChainListener.getBlockObservable()
            .map { observable ->
                observable.flatMapIterable { irohaHandler.parseBlock(it) }
            }
    }

    /**
     * Init Notary
     */
    private fun initNotary(ethEvents: Observable<NotaryInputEvent>, irohaEvents: Observable<NotaryInputEvent>) {
        logger.info { "Init Notary notary" }
        notary = NotaryImpl(ethEvents, irohaEvents)
    }

    /**
     * Init Iroha consumer
     */
    private fun initIrohaConsumer(): Result<Unit, Exception> {
        logger.info { "Init Iroha consumer" }
        return IrohaKeyLoader.loadKeypair(CONFIG[ConfigKeys.pubkeyPath], CONFIG[ConfigKeys.privkeyPath])
            .map {
                irohaConsumer = IrohaConsumerImpl(it)

                // Init Iroha Consumer pipeline
                notary.irohaOutput()
                    // convert from Notary model to Iroha model
                    // TODO rework Iroha batch transaction
                    .flatMapIterable { irohaConverter.convert(it) }
                    // convert from Iroha model to Protobuf representation
                    .map { irohaConsumer.convertToProto(it) }
                    .subscribe(
                        // send to Iroha network layer
                        { irohaNetwork.send(it) },
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
        // TODO 18/05/2018, @muratovv: rework eth strategy with effective implementation
        refundServerEndpoint = RefundServerEndpoint(
            ServerInitializationBundle(CONFIG[ConfigKeys.refundPort], CONFIG[ConfigKeys.ethEndpoint]),
            mock {
                val request = any<EthRefundRequest>()
                on {
                    performRefund(request)
                } doReturn EthNotaryResponse.Successful(
                    "signature",
                    EthRefund("address", "coin", BigInteger.TEN)
                )
            })
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
