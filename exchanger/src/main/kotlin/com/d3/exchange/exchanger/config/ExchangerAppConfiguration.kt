/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger.config

import com.d3.chainadapter.client.RMQConfig
import com.d3.chainadapter.client.ReliableIrohaChainListener
import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.exchange.exchanger.context.CurveExchangerContext
import com.d3.exchange.exchanger.context.DcExchangerContext
import com.d3.exchange.exchanger.strategy.CurveRateStrategy
import com.d3.exchange.exchanger.strategy.DcRateStrategy
import com.d3.exchange.exchanger.util.TradingPairsHelper
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import jp.co.soramitsu.iroha.java.Utils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.math.BigDecimal

val exchangerConfig =
    loadRawLocalConfigs(
        "exchanger",
        ExchangerConfig::class.java,
        "exchanger.properties"
    )

val rmqConfig = loadRawLocalConfigs("rmq", RMQConfig::class.java, "rmq.properties")

val exchangerCurveConfig =
    loadRawLocalConfigs(
        "exchanger",
        ExchangerCurveContextConfig::class.java,
        "exchanger-curve.properties"
    )

val exchangerDcConfig =
    loadRawLocalConfigs(
        "exchanger",
        ExchangerDcConfig::class.java,
        "exchanger-dc.properties"
    )

const val EXCHANGER_SERVICE_NAME = "exchanger-service"

/**
 * Spring configuration for Notary Exchanger Service
 */
@Configuration
class ExchangerAppConfiguration {

    @Bean
    fun irohaAPI() = IrohaAPI(exchangerConfig.iroha.hostname, exchangerConfig.iroha.port)

    @Bean
    fun feeFraction() = BigDecimal(exchangerConfig.feeFraction)

    @Bean
    fun liquidityProviders() = exchangerConfig.liquidityProviders.split(",").toList()

    @Bean
    fun curveIrohaCredential() = IrohaCredential(exchangerCurveConfig.exchangerIrohaCredential)

    @Bean
    fun curveIrohaConsumer() =
        IrohaConsumerImpl(
            curveIrohaCredential(),
            irohaAPI()
        )

    @Bean
    fun curveAccountId() = exchangerCurveConfig.exchangerIrohaCredential.accountId

    @Bean
    fun curveQueryAPI() =
        QueryAPI(
            irohaAPI(),
            curveAccountId(),
            Utils.parseHexKeypair(
                exchangerCurveConfig.exchangerIrohaCredential.pubkey,
                exchangerCurveConfig.exchangerIrohaCredential.privkey
            )
        )

    @Bean
    fun curveQueryHelper() = IrohaQueryHelperImpl(curveQueryAPI())

    @Bean
    fun curveRateStrategy() =
        CurveRateStrategy(
            curveAccountId(),
            curveQueryHelper(),
            feeFraction()
        )

    @Bean
    fun curveTradingPairsHelper() =
        TradingPairsHelper(
            exchangerConfig.tradePairSetter,
            exchangerConfig.tradePairKey,
            curveAccountId(),
            curveQueryHelper()
        )

    @Bean
    fun curveExchangerContext() =
        CurveExchangerContext(
            curveIrohaConsumer(),
            curveQueryHelper(),
            curveRateStrategy(),
            liquidityProviders(),
            curveTradingPairsHelper()
        )

    @Bean
    fun dcIrohaCredential() = IrohaCredential(exchangerDcConfig.exchangerIrohaCredential)

    @Bean
    fun dcIrohaConsumer() =
        IrohaConsumerImpl(
            dcIrohaCredential(),
            irohaAPI()
        )

    @Bean
    fun dcAccountId() = exchangerDcConfig.exchangerIrohaCredential.accountId

    @Bean
    fun dcQueryAPI() =
        QueryAPI(
            irohaAPI(),
            dcAccountId(),
            Utils.parseHexKeypair(
                exchangerDcConfig.exchangerIrohaCredential.pubkey,
                exchangerDcConfig.exchangerIrohaCredential.privkey
            )
        )

    @Bean
    fun dcQueryHelper() = IrohaQueryHelperImpl(dcQueryAPI())

    @Bean
    fun dcRateStrategy() =
        DcRateStrategy(
            exchangerDcConfig.assetRateBaseUrl,
            exchangerDcConfig.baseAssetId,
            exchangerDcConfig.rateAttribute,
            feeFraction()
        )

    @Bean
    fun dcTradingPairsHelper() =
        TradingPairsHelper(
            exchangerConfig.tradePairSetter,
            exchangerConfig.tradePairKey,
            dcAccountId(),
            dcQueryHelper()
        )

    @Bean
    fun dcExchangerContext() =
        DcExchangerContext(
            dcIrohaConsumer(),
            dcQueryHelper(),
            dcRateStrategy(),
            liquidityProviders(),
            dcTradingPairsHelper()
        )

    @Bean
    fun contexts() = listOf(curveExchangerContext(), dcExchangerContext())

    @Bean
    fun chainListener() =
        ReliableIrohaChainListener(
            rmqConfig,
            exchangerConfig.irohaBlockQueue
        )
}
