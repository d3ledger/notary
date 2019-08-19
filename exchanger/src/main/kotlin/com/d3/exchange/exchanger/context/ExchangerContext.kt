/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger.context

import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.exchange.exchanger.exceptions.AssetNotFoundException
import com.d3.exchange.exchanger.exceptions.TooLittleAssetVolumeException
import com.d3.exchange.exchanger.exceptions.UnsupportedTradingPairException
import com.d3.exchange.exchanger.strategy.RateStrategy
import com.d3.exchange.exchanger.util.TradingPairsHelper
import com.d3.exchange.exchanger.util.respectPrecision
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import mu.KLogging
import java.math.BigDecimal

/**
 * Context telling exchanger how to process inputs
 */
abstract class ExchangerContext(
    protected val irohaConsumer: IrohaConsumer,
    protected val queryHelper: IrohaQueryHelper,
    private val rateStrategy: RateStrategy,
    protected val liquidityProviderAccounts: List<String>,
    protected val tradingPairsHelper: TradingPairsHelper,
    protected val exchangerAccountId: String
) {

    /**
     * Performs conversion based on the block specified
     * @param block block to process
     */
    fun performConversions(block: BlockOuterClass.Block) {
        block.blockV1.payload.transactionsList.map { transaction ->
            tradingPairsHelper.updateTradingPairsOnBlock(transaction)
            transaction.payload.reducedPayload.commandsList.filter { command ->
                command.hasTransferAsset()
                        && !liquidityProviderAccounts.contains(command.transferAsset.srcAccountId)
                        && command.transferAsset.destAccountId == exchangerAccountId
            }.map { command ->
                command.transferAsset
            }
        }.map { exchangeCommands ->
            exchangeCommands.forEach { exchangeCommand ->
                performConversion(exchangeCommand)
            }
        }
    }

    /**
     * Performs checking of data and converts assets using specified [RateStrategy]
     * If something goes wrong performs a rollback
     */
    private fun performConversion(exchangeCommand: Commands.TransferAsset) {
        val sourceAsset = exchangeCommand.assetId
        val targetAsset = exchangeCommand.description
        val amount = exchangeCommand.amount
        val destAccountId = exchangeCommand.srcAccountId
        logger.info { "Got a conversion request from $destAccountId: $amount $sourceAsset to $targetAsset." }
        Result.of {
            val precision = queryHelper.getAssetPrecision(targetAsset).fold(
                { it },
                { throw AssetNotFoundException("Seems the asset $targetAsset does not exist.", it) }
            )

            val sourceTrades = tradingPairsHelper.tradingPairs[sourceAsset]
            if (sourceTrades == null || !sourceTrades.contains(targetAsset)) {
                throw UnsupportedTradingPairException("Not supported trading pair: $sourceAsset -> $targetAsset")
            }

            val relevantAmount =
                rateStrategy.getAmount(sourceAsset, targetAsset, BigDecimal(amount))

            val respectPrecision =
                respectPrecision(relevantAmount.toPlainString(), precision)

            // If the result is not bigger than zero
            if (BigDecimal(respectPrecision) <= BigDecimal.ZERO) {
                throw TooLittleAssetVolumeException("Asset supplement is too low for specified conversion")
            }

            performTransferLogic(exchangeCommand, respectPrecision)

        }.fold(
            { logger.info { "Successfully converted $amount of $sourceAsset to $targetAsset." } },
            {
                logger.error("Exchanger error occurred. Performing rollback.", it)

                ModelUtil.transferAssetIroha(
                    irohaConsumer,
                    exchangerAccountId,
                    destAccountId,
                    sourceAsset,
                    "Conversion rollback transaction",
                    amount
                ).failure { ex ->
                    logger.error("Error during rollback", ex)
                }
            })
    }

    /**
     * Customizable transfer logic
     */
    protected open fun performTransferLogic(originalCommand: Commands.TransferAsset, amount: String) {
        val sourceAsset = originalCommand.assetId
        val targetAsset = originalCommand.description
        val destAccountId = originalCommand.srcAccountId

        ModelUtil.transferAssetIroha(
            irohaConsumer,
            exchangerAccountId,
            destAccountId,
            targetAsset,
            "Conversion from $sourceAsset to $targetAsset",
            amount
        ).failure {
            throw it
        }
    }

    companion object : KLogging()
}
