package notary

import endpoint.RefundEndpoint
import main.Configs
import mu.KLogging
import sideChain.eth.EthChainHandlerStub
import sideChain.eth.EthChainListenerStub
import sideChain.iroha.IrohaChainHandlerStub
import sideChain.iroha.IrohaChainListenerStub
import sideChain.iroha.consumer.IrohaConsumer
import sideChain.iroha.consumer.IrohaConsumerImpl
import sideChain.iroha.consumer.IrohaCryptoProviderImpl
import sideChain.iroha.consumer.IrohaNetworkImpl

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

    // ------------------------------------------| Notary |------------------------------------------
    private lateinit var notary: Notary

    /**
     * Init notary
     */
    fun init() {
        logger.info { "Notary initialization" }
        initEthChain()
        initIrohaChain()
        initNotary()
        initIrohaConsumer()
        initRefund()
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
    fun initIrohaConsumer() {
        logger.info { "Init Iroha consumer" }
        val cryptoProvider = IrohaCryptoProviderImpl(Configs.pubkeyPath, Configs.privkeyPath)
        irohaConsumer = IrohaConsumerImpl(notary.irohaOutput(), cryptoProvider, IrohaNetworkImpl())
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
