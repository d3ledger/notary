/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.util

import com.d3.chainadapter.client.ReliableIrohaChainListener
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.util.createPrettySingleThreadPool
import com.d3.commons.util.getRandomString
import com.d3.exchange.exchanger.config.EXCHANGER_SERVICE_NAME
import com.d3.exchange.exchanger.config.rmqConfig
import com.d3.exchange.exchanger.context.CurveExchangerContext
import com.d3.exchange.exchanger.service.ExchangerService
import com.d3.exchange.exchanger.strategy.CurveRateStrategy
import com.d3.exchange.exchanger.util.TradingPairsHelper
import integration.helper.IrohaIntegrationHelperUtil
import java.io.Closeable
import java.math.BigDecimal

/**
 * Environment for exchanger service running in tests
 */
class ExchangerServiceTestEnvironment(private val integrationHelper: IrohaIntegrationHelperUtil) :
    Closeable {

    val exchangerAccount = integrationHelper.accountHelper.exchangerAccount

    private val exchangerAccountId = integrationHelper.accountHelper.exchangerAccount.accountId

    private val testAccountId = integrationHelper.testCredential.accountId

    val testDetailKey = "test"

    private val exchangerCredential =
        IrohaCredential(exchangerAccount.accountId, exchangerAccount.keyPair)

    private val irohaConsumer = IrohaConsumerImpl(exchangerCredential, integrationHelper.irohaAPI)

    private val chainListener = ReliableIrohaChainListener(
        rmqConfig,
        "exchanger_blocks_${String.getRandomString(5)}",
        createPrettySingleThreadPool(EXCHANGER_SERVICE_NAME, "rmq-consumer")
    )

    private val queryHelper = integrationHelper.queryHelper

    private lateinit var exchangerService: ExchangerService

    fun init() {
        exchangerService = ExchangerService(
            chainListener,
            listOf(
                CurveExchangerContext(
                    irohaConsumer,
                    queryHelper,
                    CurveRateStrategy(
                        exchangerAccountId,
                        queryHelper,
                        BigDecimal(0.99)
                    ),
                    listOf(testAccountId),
                    TradingPairsHelper(
                        testAccountId,
                        testDetailKey,
                        exchangerAccountId,
                        queryHelper
                    )
                )
            )
        )
        exchangerService.start()
    }

    override fun close() {
        exchangerService.close()
        integrationHelper.close()
    }
}
