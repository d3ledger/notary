/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.util

import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.ReliableIrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.util.createPrettySingleThreadPool
import com.d3.commons.util.getRandomString
import com.d3.exchange.exchanger.EXCHANGER_SERVICE_NAME
import com.d3.exchange.exchanger.ExchangerService
import com.d3.exchange.exchanger.rmqConfig
import integration.helper.IrohaIntegrationHelperUtil
import java.io.Closeable

/**
 * Environment for exchanger service running in tests
 */
class ExchangerServiceTestEnvironment(private val integrationHelper: IrohaIntegrationHelperUtil) :
    Closeable {

    val exchangerAccount = integrationHelper.accountHelper.exchangerAccount

    private val exchangerCredential =
        IrohaCredential(exchangerAccount.accountId, exchangerAccount.keyPair)

    private val irohaConsumer = IrohaConsumerImpl(exchangerCredential, integrationHelper.irohaAPI)

    private val chainListener = ReliableIrohaChainListener(
        rmqConfig,
        "exchanger_blocks_${String.getRandomString(5)}",
        createPrettySingleThreadPool(EXCHANGER_SERVICE_NAME, "rmq-consumer")
    )

    private lateinit var exchangerService: ExchangerService

    fun init() {
        exchangerService = ExchangerService(
            irohaConsumer,
            IrohaQueryHelperImpl(
                integrationHelper.irohaAPI,
                exchangerCredential.accountId,
                exchangerAccount.keyPair
            ),
            chainListener,
            listOf(integrationHelper.testCredential.accountId)
        )
        exchangerService.start()
    }

    override fun close() {
        exchangerService.close()
        integrationHelper.close()
    }
}
