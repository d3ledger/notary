package com.d3.exchange.util

import com.d3.commons.config.RMQConfig
import com.d3.commons.config.getConfigFolder
import com.d3.commons.config.loadRawConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.ReliableIrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.util.getRandomString
import com.d3.exchange.exchanger.ExchangerService
import integration.helper.IrohaIntegrationHelperUtil
import java.io.Closeable

/**
 * Environment for exchanger service running in tests
 */
class ExchangerServiceTestEnvironment(private val integrationHelper: IrohaIntegrationHelperUtil) :
    Closeable {

    private val rmqConfig =
        loadRawConfigs("rmq", RMQConfig::class.java, "${getConfigFolder()}/rmq.properties")

    val exchangerAccount = integrationHelper.accountHelper.exchangerAccount

    private val exchangerCredential =
        IrohaCredential(exchangerAccount.accountId, exchangerAccount.keyPair)

    private val irohaConsumer = IrohaConsumerImpl(exchangerCredential, integrationHelper.irohaAPI)

    private lateinit var exchangerService: ExchangerService

    fun init() {
        exchangerService = ExchangerService(
            irohaConsumer,
            IrohaQueryHelperImpl(
                integrationHelper.irohaAPI,
                exchangerCredential.accountId,
                exchangerAccount.keyPair
            ),
            ReliableIrohaChainListener(rmqConfig, "exchanger_blocks_${String.getRandomString(5)}"),
            listOf(integrationHelper.testCredential.accountId)
        )
        exchangerService.start()
    }

    override fun close() {
        exchangerService.close()
        integrationHelper.close()
    }
}
