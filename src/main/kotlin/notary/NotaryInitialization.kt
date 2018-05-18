package notary

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import endpoint.RefundEndpoint
import io.reactivex.Observable
import main.CONFIG
import main.ConfigKeys
import mu.KLogging
import sideChain.eth.EthChainHandler
import sideChain.eth.EthChainListener
import sideChain.iroha.IrohaChainHandlerStub
import sideChain.iroha.IrohaChainListenerStub
import sideChain.iroha.consumer.*

/**
 * Class for notary instantiation
 */
class NotaryInitialization {

    private lateinit var refundEndpoint: RefundEndpoint

    // ------------------------------------------| ETH |------------------------------------------
    private val ethChainListener = EthChainListener()
    private val ethHandler = EthChainHandler()

    // ------------------------------------------| Iroha |------------------------------------------
    private var irohaChainListener = IrohaChainListenerStub()
    private val irohaHandler = IrohaChainHandlerStub()

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
            .map { initNotary(it.first, it.second) }
            .flatMap { initIrohaConsumer() }
            .map { initRefund() }
    }

    /**
     * Init Ethereum chain listener
     * @return Observable on Ethereum sidechain events
     */
    fun initEthChain(): Result<Observable<NotaryEvent>, Exception> {
        logger.info { "Init Eth chain" }
        return ethChainListener.getBlockObservable()
            .map { observable ->
                observable.flatMapIterable { ethHandler.parseBlock(it) }
            }
    }

    /**
     * Init Iroha chain listener
     * @return Observable on Iroha sidechain events
     */
    fun initIrohaChain(): Result<Observable<NotaryEvent>, Exception> {
        logger.info { "Init Iroha chain" }
        return irohaChainListener.getBlockObservable()
            .map { observable ->
                observable.flatMapIterable { irohaHandler.parseBlock(it) }
            }
    }

    /**
     * Init Notary
     */
    fun initNotary(ethEvents: Observable<NotaryEvent>, irohaEvents: Observable<NotaryEvent>) {
        logger.info { "Init Notary notary" }
        notary = NotaryStub(ethEvents, irohaEvents)
    }

    /**
     * Init Iroha consumer
     */
    fun initIrohaConsumer(): Result<Unit, Exception> {
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
    fun initRefund() {
        logger.info { "Init Refund endpoint" }
        refundEndpoint = RefundEndpoint()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
