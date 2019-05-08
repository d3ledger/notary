/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange.exchanger

import com.d3.commons.sidechain.iroha.ReliableIrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.exchange.exchanger.exception.AssetNotFoundException
import com.d3.exchange.exchanger.exception.TooLittleAssetVolumeException
import com.d3.exchange.exchanger.exception.TooMuchAssetVolumeException
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import mu.KLogging
import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.integration.RombergIntegrator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.Closeable
import java.math.BigDecimal

private val logger = KLogging().logger

@Component
/**
 * Class responsible for Iroha block processing and proper reacting to incoming transfers
 * in order to support automatic asset conversions
 */
class ExchangerService(
    @Autowired private val irohaConsumer: IrohaConsumer,
    @Autowired private val queryhelper: IrohaQueryHelper,
    @Autowired private val chainListener: ReliableIrohaChainListener,
    @Autowired private val liquidityProviderAccounts: List<String>
) : Closeable {

    // Exchanger account
    private val exchangerAccountId = irohaConsumer.creator

    /**
     * Starts blocks listening and processing
     */
    fun start(): Result<Unit, Exception> {
        logger.info { "Exchanger service is started. Waiting for incoming transactions." }
        return chainListener.getBlockObservable().map { observable ->
            observable.subscribe { (block, _) -> processBlock(block) }
            Unit
        }
    }

    /**
     * Filters incoming transfers commands and calls conversion for them
     * Ignores transactions from 'liquidity providers'
     * They are not supposed to receive conversion
     */
    private fun processBlock(block: BlockOuterClass.Block) {
        block.blockV1.payload.transactionsList.map { transaction ->
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
     * Performs conversion based on the command specified
     * Conversion is just an outgoing transfer transaction
     * If something goes wrong rollback is performed
     */
    private fun performConversion(transfer: Commands.TransferAsset) {
        val sourceAsset = transfer.assetId
        val targetAsset = transfer.description
        val amount = transfer.amount
        val destAccountId = transfer.srcAccountId
        logger.info { "Got a conversion request from $destAccountId: $amount $sourceAsset to $targetAsset." }
        Result.of {
            val relevantAmount =
                calculateRelevantAmount(sourceAsset, targetAsset, BigDecimal(amount))

            ModelUtil.transferAssetIroha(
                irohaConsumer,
                exchangerAccountId,
                destAccountId,
                targetAsset,
                "Conversion from $sourceAsset to $targetAsset",
                relevantAmount
            )
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
                )
            })
    }

    /**
     * Uses integration to calculate how much assets should be sent back to the client
     * Logic is not to fix a rate in a moment but to determine continuous rate for
     * any amount of assets.
     * @throws AssetNotFoundException in case of unknown target asset
     * @throws TooMuchAssetVolumeException in case of impossible conversion when supplied too much
     * @throws TooLittleAssetVolumeException in case of impossible conversion when supplied too little
     */
    private fun calculateRelevantAmount(from: String, to: String, amount: BigDecimal): String {
        val sourceAssetBalance = BigDecimal(
            queryhelper.getAccountAsset(irohaConsumer.creator, from).get()
        ).minus(amount).toDouble()
        val targetAssetBalance =
            BigDecimal(queryhelper.getAccountAsset(irohaConsumer.creator, to).get()).toDouble()
        val amountMinusFee = amount.minus(amount.multiply(FEE_RATIO)).toDouble()

        val precision = queryhelper.getAssetPrecision(to).fold(
            { it },
            { throw AssetNotFoundException("Seems asset $to does not exist.") })

        val calculatedAmount = integrate(sourceAssetBalance, targetAssetBalance, amountMinusFee)

        if (calculatedAmount >= targetAssetBalance) {
            throw TooMuchAssetVolumeException("Asset supplement exceeds the balance.")
        }

        val respectPrecision =
            respectPrecision(BigDecimal(calculatedAmount).toPlainString(), precision)
        // If the result is not bigger than zero
        if (BigDecimal(respectPrecision) <= BigDecimal.ZERO) {
            throw TooLittleAssetVolumeException("Asset supplement it too low for specified conversion")
        }
        return respectPrecision
    }

    /**
     * Normalizes a string to an asset precision
     * @return String {0-9}*.{0-9}[precision]
     */
    private fun respectPrecision(rawValue: String, precision: Int): String {
        val substringAfter = rawValue.substringAfter(DELIMITER)
        val diff = substringAfter.length - precision
        return when {
            diff == 0 -> rawValue
            diff < 0 -> rawValue.plus("0".repeat(diff * (-1)))
            else -> rawValue.substringBefore(DELIMITER)
                .plus(DELIMITER)
                .plus(substringAfter.substring(0, precision))
        }
    }

    override fun close() {
        chainListener.close()
    }

    companion object {
        // Number of evaluations during integration
        private const val EVALUATIONS = 1000
        // Integrating from the relevant rate which is at x=0
        private const val LOWER_BOUND = 0.0
        // 1% so far
        private val FEE_RATIO = BigDecimal(0.01)
        private const val DELIMITER = '.'

        // Integrals
        private val integrator = RombergIntegrator()

        /**
         * Performs integration of target asset amount function
         */
        fun integrate(
            sourceAssetBalance: Double,
            targetAssetBalance: Double,
            amount: Double
        ): Double {
            val function =
                UnivariateFunction { x -> targetAssetBalance / (sourceAssetBalance + x + 1) }
            return integrator.integrate(EVALUATIONS, function, LOWER_BOUND, LOWER_BOUND + amount)
        }
    }
}
