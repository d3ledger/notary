package com.d3.exchange.exchanger

import com.d3.commons.sidechain.iroha.ReliableIrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import jp.co.soramitsu.iroha.java.Transaction
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
): Closeable {
    override fun close() {
        chainListener.close()
    }

    private val exchangerAccountId = irohaConsumer.creator

    fun start(): Result<Unit, Exception> {
        logger.info { "Exchanger service is starting." }
        return chainListener.getBlockObservable().map { observable ->
            observable.blockingSubscribe { (block, _) ->
                processBlock(block)
            }
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
                irohaConsumer.send(
                    constructResponseTransaction(exchangeCommand)
                )
            }
        }
    }

    private fun constructResponseTransaction(transfer: Commands.TransferAsset): Transaction {
        return Transaction.builder(exchangerAccountId).transferAsset(
            exchangerAccountId,
            transfer.srcAccountId,
            transfer.description,
            "Conversion from ${transfer.assetId} to ${transfer.description}",
            calculateRelevantOutcome()
        ).build()
    }

    private fun calculateRelevantOutcome(): BigDecimal {
        // TODO CALCULATE
        return BigDecimal.ONE
    }
}