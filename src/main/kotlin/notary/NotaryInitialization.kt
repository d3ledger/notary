package notary

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import endpoint.RefundEndpoint
import io.reactivex.Observable
import main.Configs
import mu.KLogging
import sideChain.eth.EthChainHandlerStub
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
    private lateinit var ethChainListener: EthChainListener
    private val ethHandler = EthChainHandlerStub()

    // ------------------------------------------| Iroha |------------------------------------------
    private lateinit var irohaConsumer: IrohaConsumer
    private lateinit var irohaChainListener: IrohaChainListenerStub
    private val irohaHandler = IrohaChainHandlerStub()

    private val irohaConverter = IrohaConverterImpl()
    private val irohaNetwork = IrohaNetworkImpl()

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
            .map { initIrohaConsumer() }
            .map { initRefund() }
    }

    /**
     * Init Ethereum chain listener
     * @return Observable on Ethereum sidechain events
     */
    fun initEthChain(): Result<Observable<NotaryEvent>, Exception> {
        logger.info { "Init Eth chain" }
        ethChainListener = EthChainListener()
        return ethChainListener.getBlockObservable()
            .map { observable ->
                observable.map { ethHandler.parseBlock(it) }
            }
    }

    /**
     * Init Iroha chain listener
     * @return Observable on Iroha sidechain events
     */
    fun initIrohaChain(): Result<Observable<NotaryEvent>, Exception> {
        logger.info { "Init Iroha chain" }
        irohaChainListener = IrohaChainListenerStub()
        return irohaChainListener.getBlockObservable()
            .map { observable ->
                observable.map { irohaHandler.parseBlock(it) }
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
        return IrohaKeyLoader.loadKeypair(Configs.pubkeyPath, Configs.privkeyPath)
            .map {
                irohaConsumer = IrohaConsumerImpl(it)

                // Init Iroha Consumer pipeline
                notary.irohaOutput()
                    // convert from Notary model to Iroha model
                    // TODO rework Iroha batch transaction
                    .flatMapIterable { irohaConverter.convert(it) }
                    // convert from Iroha model to Protobuf representation
                    .map { irohaConsumer.convertToProto(it) }
                    // send to Iroha network layer
                    .subscribe { irohaNetwork.send(it) }
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
