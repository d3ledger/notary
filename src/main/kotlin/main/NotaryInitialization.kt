package main

import algorithm.IrohaConsumer
import algorithm.IrohaConsumerStub
import algorithm.Notary
import algorithm.NotaryStub
import endpoint.RefundEndpoint
import mu.KLogging
import sideChain.eth.EthChainHandlerStub
import sideChain.eth.EthChainListenerStub

/**
 * Class for notary instantiation
 */
class NotaryInitialization {

    private lateinit var refundEndpoint: RefundEndpoint

    // ------------------------------------------| ETH |------------------------------------------

    private lateinit var ethChainHandler: EthChainListenerStub

    private lateinit var ethHandler: EthChainHandlerStub


    // ------------------------------------------| Notary |------------------------------------------

    private lateinit var notary: Notary

    // ------------------------------------------| Iroha |------------------------------------------

    private lateinit var iroha: IrohaConsumer

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
        ethChainHandler = EthChainListenerStub()
        ethHandler = EthChainHandlerStub(ethChainHandler)
    }

    /**
     * Init Iroha chain
     */
    fun initIrohaChain() {
        logger.info { "Init Iroha chain" }
    }

    /**
     * Init Notary
     */
    fun initNotary() {
        logger.info { "Init Notary algorithm" }
        notary = NotaryStub(ethHandler)
    }

    /**
     * Init Iroha consumer
     */
    fun initIrohaConsumer() {
        logger.info { "Init Iroha consumer" }
        iroha = IrohaConsumerStub(notary)
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
