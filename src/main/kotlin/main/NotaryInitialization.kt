package main

import algorithm.IrohaConsumer
import algorithm.IrohaConsumerStub
import algorithm.Notary
import algorithm.NotaryStub
import mu.KLogging
import sideChain.eth.EthChainHandlerStub
import sideChain.eth.EthChainListenerStub


/**
 * Class for notary instantiation
 */
class NotaryInitialization {
    /**
     * Logger
     */
    companion object : KLogging()


    // ------------------------------------------| ETH |------------------------------------------


    private lateinit var ethChainHandler: EthChainListenerStub

    private lateinit var ethHandler: EthChainHandlerStub


    // ------------------------------------------| Notary |------------------------------------------


    private lateinit var notary: Notary

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
    }

    fun initEthChain() {
        logger.info { "Eth chain" }
        ethChainHandler = EthChainListenerStub()
    }

    fun initIrohaChain() {
        logger.info { "Iroha chain" }
        ethHandler = EthChainHandlerStub(ethChainHandler)
    }

    fun initNotary() {
        logger.info { "Notary algorithm" }
        notary = NotaryStub(ethHandler)
    }

    fun initIrohaConsumer() {
        logger.info { "Iroha consumer" }
        iroha = IrohaConsumerStub(notary)
    }
}
