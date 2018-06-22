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
import main.ConfigKeys
import mu.KLogging
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import sideChain.eth.EthChainHandler
import sideChain.eth.EthChainListener
import sideChain.iroha.IrohaChainHandler
import sideChain.iroha.IrohaChainListener
import sideChain.iroha.consumer.*
import withdrawalservice.WithdrawalServiceInitialization
import java.math.BigInteger

/**
 * Class for notary instantiation
 */
class NotaryInitialization {

    // ------------------------------------------| ETH |------------------------------------------
    /**
     * List of all observable wallets
     */
    //TODO load from file
    private val wallets = mapOf(
        "0x00Bd138aBD70e2F00903268F3Db08f2D25677C9e".toLowerCase() to "admin@notary",
        "0x00aa39d30f0d20ff03a22ccfc30b7efbfca597c2".toLowerCase() to "user1@notary"
    )

    /**
     * List of all observable ERC20 tokens
     */
    //TODO load from file
    private val tokenList = mapOf(
        "0xeDFC9c2F4Cfa7495c1A95CfE1cB856F5980D5e18".toLowerCase() to "tkn"
    )

    /**
     * Init notary
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Notary initialization" }
        return initEthChain()
            .fanout { initIrohaChain() }
            .map { initNotary(it.first, it.second) }
            .flatMap { initIrohaConsumer(it) }
            .map { initRefund() }
    }

    /**
     * Init Ethereum chain listener
     * @return Observable on Ethereum sidechain events
     */
    private fun initEthChain(): Result<Observable<NotaryInputEvent>, Exception> {
        logger.info { "Init Eth chain" }

        val web3 = Web3j.build(HttpService(CONFIG[ConfigKeys.ethConnectionUrl]))
        return EthChainListener(web3).getBlockObservable()
            .map { observable ->
                observable.flatMapIterable { EthChainHandler(web3, wallets, tokenList).parseBlock(it) }
            }
    }

    /**
     * Init Iroha chain listener
     * @return Observable on Iroha sidechain events
     */
    private fun initIrohaChain(): Result<Observable<NotaryInputEvent>, Exception> {
        logger.info { "Init Iroha chain" }
        return IrohaChainListener().getBlockObservable()
            .map { observable ->
                observable.flatMapIterable { IrohaChainHandler().parseBlock(it) }
            }
    }

    /**
     * Init Notary
     */
    private fun initNotary(ethEvents: Observable<NotaryInputEvent>, irohaEvents: Observable<NotaryInputEvent>): Notary {
        logger.info { "Init Notary notary" }
        return NotaryImpl(ethEvents, irohaEvents)
    }

    /**
     * Init Iroha consumer
     */
    private fun initIrohaConsumer(notary: Notary): Result<Unit, Exception> {
        logger.info { "Init Iroha consumer" }
        return IrohaKeyLoader.loadKeypair(CONFIG[ConfigKeys.pubkeyPath], CONFIG[ConfigKeys.privkeyPath])
            .map {
                val irohaConsumer = IrohaConsumerImpl(it)

                // Init Iroha Consumer pipeline
                notary.irohaOutput()
                    // convert from Notary model to Iroha model
                    // TODO rework Iroha batch transaction
                    .flatMapIterable { IrohaConverterImpl().convert(it) }
                    // convert from Iroha model to Protobuf representation
                    .map { irohaConsumer.convertToProto(it) }
                    .subscribe(
                        // send to Iroha network layer
                        { IrohaNetworkImpl().send(it) },
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
        RefundServerEndpoint(
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
