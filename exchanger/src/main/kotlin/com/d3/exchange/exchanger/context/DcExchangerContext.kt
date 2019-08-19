/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger.context

import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.exchange.exchanger.strategy.DcRateStrategy
import com.d3.exchange.exchanger.util.TradingPairsHelper
import com.github.kittinunf.result.failure
import iroha.protocol.Commands
import jp.co.soramitsu.iroha.java.Transaction

/**
 * [ExchangerContext] implementation that uses [DcRateStrategy]
 */
class DcExchangerContext(
    irohaConsumer: IrohaConsumer,
    queryhelper: IrohaQueryHelper,
    dcRateStrategy: DcRateStrategy,
    liquidityProviderAccounts: List<String>,
    tradingPairsHelper: TradingPairsHelper
) : ExchangerContext(
    irohaConsumer,
    queryhelper,
    dcRateStrategy,
    liquidityProviderAccounts,
    tradingPairsHelper,
    irohaConsumer.creator
) {

    /**
     * Burns and mints corresponding fiat currencies together with sending
     */
    override fun performTransferLogic(originalCommand: Commands.TransferAsset, amount: String) {
        val sourceAsset = originalCommand.assetId
        val srcAmount = originalCommand.amount
        val targetAsset = originalCommand.description
        val destAccountId = originalCommand.srcAccountId

        val transactionBuilder = Transaction.builder(exchangerAccountId)
        if (sourceAsset != XOR_ASSET_ID) {
            transactionBuilder.subtractAssetQuantity(sourceAsset, srcAmount)
        }
        if (destAccountId != XOR_ASSET_ID) {
            transactionBuilder.addAssetQuantity(targetAsset, amount)
        }
        irohaConsumer.send(
            transactionBuilder
                .transferAsset(
                    exchangerAccountId,
                    destAccountId,
                    targetAsset,
                    "Conversion from $sourceAsset to $targetAsset",
                    amount
                )
                .build()
        ).failure {
            throw it
        }
    }

    companion object {
        private const val XOR_ASSET_ID = "xor#sora"
    }
}
