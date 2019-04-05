package com.d3.exchange.exchanger

import com.d3.commons.sidechain.iroha.ReliableIrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.google.common.base.Strings
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.Closeable
import java.math.BigDecimal

private val logger = KLogging().logger

@Component
class ExchangerService(
    @Autowired private val irohaConsumer: IrohaConsumer,
    @Autowired private val chainListener: ReliableIrohaChainListener
) : Closeable {

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
                command.hasTransferAsset() && command.transferAsset.destAccountId == exchangerAccountId
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
        val targetAsset = transfer.description
        if (!Strings.isNullOrEmpty(targetAsset)) {
            ModelUtil.transferAssetIroha(
                irohaConsumer,
                exchangerAccountId,
                transfer.srcAccountId,
                targetAsset,
                "Conversion from ${transfer.assetId} to $targetAsset",
                calculateRelevantOutcome()
            )
        }
    }

    private fun calculateRelevantOutcome(): String {
        // TODO CALCULATE
        return BigDecimal.ONE.toPlainString()
    }

    override fun close() {
        chainListener.close()
    }
}