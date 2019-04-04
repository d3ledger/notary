package com.d3.exchange.exchanger

import com.d3.commons.config.RMQConfig
import com.d3.commons.config.loadConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.ReliableIrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.createPrettySingleThreadPool
import jp.co.soramitsu.iroha.java.IrohaAPI
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

val exchangerConfig =
    loadConfigs("exchanger", ExchangerConfig::class.java, "/exchanger.properties").get()

val rmqConfig = loadConfigs("rmq", RMQConfig::class.java, "/rmq.properties").get()

const val EXCHANGER_SERVICE_NAME = "exchanger-service"

/**
 * Spring configuration for Notary Exchanger Service
 */
@Configuration
class ExchangerAppConfiguration {

    /** Exchanger service credentials */
    private val exchangerCredential = ModelUtil.loadKeypair(
        exchangerConfig.irohaCredential.pubkeyPath,
        exchangerConfig.irohaCredential.privkeyPath
    ).fold(
        { keypair ->
            IrohaCredential(exchangerConfig.irohaCredential.accountId, keypair)
        },
        { ex -> throw ex }
    )

    /** Iroha network connection */
    @Bean
    fun irohaAPI() = IrohaAPI(exchangerConfig.iroha.hostname, exchangerConfig.iroha.port)

    @Bean
    fun irohaConsumer() = IrohaConsumerImpl(
        exchangerCredential, irohaAPI()
    )

    /** Configurations for Exchanger Service */
    @Bean
    fun registrationConfig() = exchangerConfig

    @Bean
    fun chainListener() = ReliableIrohaChainListener(
        rmqConfig,
        exchangerConfig.irohaBlockQueue,
        createPrettySingleThreadPool(EXCHANGER_SERVICE_NAME, "rmq-consumer")
    )
}
