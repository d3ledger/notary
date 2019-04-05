package com.d3.exchange.util

import com.d3.commons.config.RMQConfig
import com.d3.commons.config.loadConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.ReliableIrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.exchange.exchanger.ExchangerConfig
import com.d3.exchange.exchanger.ExchangerService
import integration.helper.IrohaIntegrationHelperUtil
import java.io.Closeable

/**
 * Environment for exchanger service running in tests
 */
class ExchangerServiceTestEnvironment(private val integrationHelper: IrohaIntegrationHelperUtil) : Closeable {

    private val exchangerConfig =
        loadConfigs("exchanger", ExchangerConfig::class.java, "/exchanger.properties").get()

    private val rmqConfig = loadConfigs("rmq", RMQConfig::class.java, "/rmq.properties").get()

    private val registrationCredentials = ModelUtil.loadKeypair(
        exchangerConfig.irohaCredential.pubkeyPath,
        exchangerConfig.irohaCredential.privkeyPath
    ).fold(
        { keypair ->
            IrohaCredential(exchangerConfig.irohaCredential.accountId, keypair)
        },
        { ex -> throw ex }
    )

    private val irohaConsumer = IrohaConsumerImpl(registrationCredentials, integrationHelper.irohaAPI)

    private lateinit var exchangerService: ExchangerService

    fun init() {
        exchangerService = ExchangerService(
            irohaConsumer,
            ReliableIrohaChainListener(rmqConfig, exchangerConfig.irohaBlockQueue)
        )
        exchangerService.start()
    }

    val exchangerAccountId = exchangerConfig.irohaCredential.accountId

    override fun close() {
        exchangerService.close()
        integrationHelper.close()
    }
}
