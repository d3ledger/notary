package com.d3.exchange.exchanger

import com.d3.commons.sidechain.iroha.ReliableIrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.sidechain.iroha.util.getAccountAsset
import com.d3.commons.sidechain.iroha.util.getAssetPrecision
import com.d3.exchange.exchanger.exception.AssetNotFoundException
import com.d3.exchange.exchanger.exception.TooMuchAssetVolumeException
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import jp.co.soramitsu.iroha.java.QueryAPI
import mu.KLogging
import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.integration.RombergIntegrator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.Closeable
import java.math.BigDecimal

private val logger = KLogging().logger

@Component
class ExchangerService(
    @Autowired private val irohaConsumer: IrohaConsumer,
    @Autowired private val queryAPI: QueryAPI,
    @Autowired private val chainListener: ReliableIrohaChainListener,
    @Autowired private val liquidityProviderAccounts: List<String>
) : Closeable {

    // Integrals
    private val integrator = RombergIntegrator()
    private val exchangerAccountId = irohaConsumer.creator

    fun start(): Result<Unit, Exception> {
        logger.info { "Exchanger service is starting." }
        return chainListener.getBlockObservable().map { observable ->
            observable.subscribe { (block, _) -> processBlock(block) }
            Unit
        }
    }

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

    private fun performConversion(transfer: Commands.TransferAsset) {
        val sourceAsset = transfer.assetId
        val targetAsset = transfer.description
        val amount = transfer.amount
        Result.of {
            val relevantAmount = calculateRelevantAmount(sourceAsset, targetAsset, BigDecimal(amount))

            ModelUtil.transferAssetIroha(
                irohaConsumer,
                exchangerAccountId,
                transfer.srcAccountId,
                targetAsset,
                "Conversion from $sourceAsset to $targetAsset",
                relevantAmount
            )
        }.fold({ logger.info { "Successfully converted $amount of $sourceAsset to $targetAsset." } },
            {
                logger.error("Exchanger error occurred. Performing rollback.", it)

                ModelUtil.transferAssetIroha(
                    irohaConsumer,
                    exchangerAccountId,
                    transfer.srcAccountId,
                    sourceAsset,
                    "Conversion rollback transaction",
                    amount
                )
            })
    }

    private fun calculateRelevantAmount(from: String, to: String, amount: BigDecimal): String {
        val fromAsset = BigDecimal(
            getAccountAsset(queryAPI, irohaConsumer.creator, from).get()
        ).minus(amount).toDouble()
        val toAsset = BigDecimal(getAccountAsset(queryAPI, irohaConsumer.creator, to).get()).toDouble()
        val amountMinusFee = amount.minus(amount.divide(FEE_PROPORTION)).toDouble()

        val function = UnivariateFunction { x -> toAsset / (fromAsset * x) }
        val precision = getAssetPrecision(queryAPI, to).fold({ it },
            { throw AssetNotFoundException("Seems asset $to does not exist.") })

        val integrate = integrator.integrate(EVALUATIONS, function, LOWER_BOUND, LOWER_BOUND + amountMinusFee)

        if (integrate >= toAsset) {
            throw TooMuchAssetVolumeException("Asset supplement exceeds the balance.")
        }

        return respectPrecision(integrate.toString(), precision)
    }

    private fun respectPrecision(rawValue: String, precision: Int): String {
        val diff = rawValue.substringAfter('.').length - precision
        if (diff >= 0) {
            return rawValue
        }
        return rawValue.plus("0".repeat(diff * (-1)))
    }

    override fun close() {
        chainListener.close()
    }

    companion object {
        private const val EVALUATIONS = 1000
        private const val LOWER_BOUND = 1.0
        // 1% so far
        private val FEE_PROPORTION = BigDecimal.valueOf(100)!!
    }
}