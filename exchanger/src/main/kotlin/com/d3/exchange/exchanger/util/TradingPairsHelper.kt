/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger.util

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.commons.util.GsonInstance
import com.d3.commons.util.irohaUnEscape
import com.d3.exchange.exchanger.service.ExchangerService
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import com.google.gson.reflect.TypeToken
import iroha.protocol.TransactionOuterClass
import mu.KLogging

class TradingPairsHelper(
    private val tradePairSetter: String,
    private val tradePairKey: String,
    private val exchangerAccountId: String,
    private val queryHelper: IrohaQueryHelper
) {

    lateinit var tradingPairs: Map<String, Set<String>>

    init {
        getTradingPairs()
    }

    /**
     * Indicates if there was an update of the trade pairs value and loads it
     */
    fun updateTradingPairsOnBlock(transaction: TransactionOuterClass.Transaction) {
        val reducedPayload = transaction.payload.reducedPayload
        if (reducedPayload.creatorAccountId == tradePairSetter) {
            val exchangerDetails = reducedPayload.commandsList.filter { command ->
                command.hasSetAccountDetail()
                        && command.setAccountDetail.accountId == exchangerAccountId
            }.map {
                it.setAccountDetail
            }
            if (exchangerDetails.any { command -> command.key == tradePairKey }) {
                getTradingPairs()
            }
        }
    }

    /**
     * Queries Iroha for trading pairs details and loads it
     */
    private fun getTradingPairs() {
        queryHelper.getAccountDetails(exchangerAccountId, tradePairSetter, tradePairKey).map {
            val unEscape = it.get().irohaUnEscape()
            ExchangerService.logger.info { "Got trading pairs update: $unEscape" }
            tradingPairs = try {
                gson.fromJson(
                    unEscape,
                    typeToken
                )
            } catch (ex: Exception) {
                ExchangerService.logger.warn(
                    "Error parsing the value, setting trading pairs to empty",
                    ex
                )
                emptyMap()
            }
        }.failure { ex ->
            ExchangerService.logger.warn(
                "Error retrieving the value, setting trading pairs to empty",
                ex
            )
            tradingPairs = emptyMap()
        }
    }

    companion object : KLogging() {
        private val typeToken = object : TypeToken<Map<String, Set<String>>>() {}.type
        private val gson = GsonInstance.get()
    }
}
