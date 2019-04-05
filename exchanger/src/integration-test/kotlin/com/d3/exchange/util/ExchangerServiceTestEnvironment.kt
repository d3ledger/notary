package com.d3.exchange.util

import com.d3.commons.config.RMQConfig
import com.d3.commons.config.getConfigFolder
import com.d3.commons.config.loadRawConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.ReliableIrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.exchange.exchanger.ExchangerConfig
import com.d3.exchange.exchanger.ExchangerService
import integration.helper.IrohaIntegrationHelperUtil
import jp.co.soramitsu.iroha.java.QueryAPI
import java.io.Closeable

/**
 * Environment for exchanger service running in tests
 */
class ExchangerServiceTestEnvironment(private val integrationHelper: IrohaIntegrationHelperUtil) : Closeable {

    private val exchangerConfig =
        loadRawConfigs("exchanger", ExchangerConfig::class.java, "${getConfigFolder()}/exchanger.properties")

    private val rmqConfig = loadRawConfigs("rmq", RMQConfig::class.java, "${getConfigFolder()}/rmq.properties")

    val exchangerAccount = integrationHelper.accountHelper.exchangerAccount

    private val exchangerCredential = IrohaCredential(exchangerAccount.accountId, exchangerAccount.keyPair)

    private val irohaConsumer = IrohaConsumerImpl(exchangerCredential, integrationHelper.irohaAPI)

    private lateinit var exchangerService: ExchangerService

    fun init() {
        exchangerService = ExchangerService(
            irohaConsumer,
            QueryAPI(integrationHelper.irohaAPI, exchangerCredential.accountId, exchangerAccount.keyPair),
            ReliableIrohaChainListener(rmqConfig, exchangerConfig.irohaBlockQueue),
            listOf(integrationHelper.testCredential.accountId)
        )
        exchangerService.start()
    }

    override fun close() {
        exchangerService.close()
        integrationHelper.close()
    }
}
