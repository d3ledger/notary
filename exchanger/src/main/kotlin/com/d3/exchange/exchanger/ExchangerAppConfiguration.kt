/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger

import com.d3.commons.config.RMQConfig
import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.ReliableIrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.util.createPrettySingleThreadPool
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Utils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

val exchangerConfig =
    loadRawLocalConfigs(
        "exchanger",
        ExchangerConfig::class.java,
        "exchanger.properties"
    )

val rmqConfig = loadRawLocalConfigs("rmq", RMQConfig::class.java, "rmq.properties")

const val EXCHANGER_SERVICE_NAME = "exchanger-service"

/**
 * Spring configuration for Notary Exchanger Service
 */
@Configuration
class ExchangerAppConfiguration {

    private final val keyPair = Utils.parseHexKeypair(
        exchangerConfig.irohaCredential.pubkey,
        exchangerConfig.irohaCredential.privkey
    )

    /** Exchanger service credentials */
    private val exchangerCredential =
        IrohaCredential(exchangerConfig.irohaCredential.accountId, keyPair)

    /** Iroha network connection */
    @Bean
    fun irohaAPI() = IrohaAPI(exchangerConfig.iroha.hostname, exchangerConfig.iroha.port)

    @Bean
    fun irohaConsumer() = IrohaConsumerImpl(
        exchangerCredential, irohaAPI()
    )

    @Bean
    fun reliableIrohaChainListener() = ReliableIrohaChainListener(
        rmqConfig,
        exchangerConfig.irohaBlockQueue,
        createPrettySingleThreadPool(EXCHANGER_SERVICE_NAME, "rmq-consumer")
    )

    /** Configurations for Exchanger Service */
    @Bean
    fun exchangerConfig() = exchangerConfig

    @Bean
    fun queryhelper() =
        IrohaQueryHelperImpl(irohaAPI(), exchangerConfig.irohaCredential.accountId, keyPair)

    @Bean
    fun liquidityProviders() = exchangerConfig.liquidityProviders.split(",").toList()
}
