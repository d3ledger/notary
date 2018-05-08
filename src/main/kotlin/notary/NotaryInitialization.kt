package notary

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import endpoint.RefundEndpoint
import main.Configs
import mu.KLogging
import sideChain.eth.EthChainHandlerStub
import sideChain.eth.EthChainListenerStub
import sideChain.iroha.IrohaChainHandlerStub
import sideChain.iroha.IrohaChainListenerStub
import sideChain.iroha.consumer.*

/**
 * Class for notary instantiation
 */
class NotaryInitialization {

    private lateinit var refundEndpoint: RefundEndpoint

    // ------------------------------------------| ETH |------------------------------------------
    private lateinit var ethChainListener: EthChainListenerStub
    private lateinit var ethHandler: EthChainHandlerStub

    // ------------------------------------------| Iroha |------------------------------------------
    private lateinit var irohaConsumer: IrohaConsumer
    private lateinit var irohaChainListener: IrohaChainListenerStub
    private lateinit var irohaHandler: IrohaChainHandlerStub

    private val irohaConverter = IrohaConverterImpl()
    private val irohaNetwork = IrohaNetworkImpl()

    // ------------------------------------------| Notary |------------------------------------------
    private lateinit var notary: Notary

    /**
     * Init notary
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Notary initialization" }
        initEthChain()
        initIrohaChain()
        initNotary()
        return initIrohaConsumer()
            .map { initRefund() }
    }

    /**
     * Init Ethereum chain
     */
    fun initEthChain() {
        logger.info { "Init Eth chain" }
        ethChainListener = EthChainListenerStub()
        ethHandler = EthChainHandlerStub(ethChainListener)
    }

    /**
     * Init Iroha chain
     */
    fun initIrohaChain() {
        logger.info { "Init Iroha chain" }
        irohaChainListener = IrohaChainListenerStub()
        irohaHandler = IrohaChainHandlerStub(irohaChainListener)
    }

    /**
     * Init Notary
     */
    fun initNotary() {
        logger.info { "Init Notary notary" }
        notary = NotaryStub(ethHandler, irohaHandler)
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
                    .map { irohaConsumer.toProto(it) }
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
