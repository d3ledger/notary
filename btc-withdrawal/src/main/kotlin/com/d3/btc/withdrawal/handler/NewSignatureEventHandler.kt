package com.d3.btc.withdrawal.handler

import com.d3.btc.withdrawal.service.BtcRollbackService
import com.d3.btc.withdrawal.statistics.WithdrawalStatistics
import com.d3.btc.withdrawal.transaction.SignCollector
import com.d3.btc.withdrawal.transaction.TransactionHelper
import com.d3.btc.withdrawal.transaction.UnsignedTransactions
import com.d3.commons.sidechain.iroha.BTC_SIGN_COLLECT_DOMAIN
import com.github.kittinunf.result.map
import iroha.protocol.Commands
import mu.KLogging
import org.bitcoinj.core.PeerGroup
import org.bitcoinj.core.Transaction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList

/*
    Class that is used to handle new input signature appearance events
 */
@Component
class NewSignatureEventHandler(
    @Autowired private val withdrawalStatistics: WithdrawalStatistics,
    @Autowired private val signCollector: SignCollector,
    @Autowired private val unsignedTransactions: UnsignedTransactions,
    @Autowired private val transactionHelper: TransactionHelper,
    @Autowired private val btcRollbackService: BtcRollbackService
) {

    private val broadcastTransactionListeners = CopyOnWriteArrayList<(tx: Transaction) -> Unit>()

    /**
     * Registers "broadcast transaction" event listener
     * For testing purposes only.
     * @param listener - function that will be called when a transaction is about to be broadcasted
     */
    fun addBroadcastTransactionListeners(listener: (tx: Transaction) -> Unit) {
        broadcastTransactionListeners.add(listener)
    }


    /**
     * Handles "add new input signatures" commands
     * @param addNewSignatureCommand - command object full of signatures
     * @param peerGroup - group of Bitcoin peers. Used to broadcast withdraw transactions.
     * @param onBroadcastSuccess - function that will be called right after successful tx broadcast
     */
    fun handleNewSignatureCommand(
        addNewSignatureCommand: Commands.SetAccountDetail,
        peerGroup: PeerGroup,
        onBroadcastSuccess: () -> Unit
    ) {
        val shortTxHash = addNewSignatureCommand.accountId.replace("@$BTC_SIGN_COLLECT_DOMAIN", "")
        val unsignedTx = unsignedTransactions.get(shortTxHash)
        if (unsignedTx == null) {
            logger.warn { "No tx starting with hash $shortTxHash was found in collection of unsigned transactions" }
            return
        }
        val withdrawalCommand = unsignedTx.withdrawalDetails
        val tx = unsignedTx.tx
        // Hash of transaction will be changed after signing. This is why we keep an "original" hash
        val originalHash = tx.hashAsString
        signCollector.getSignatures(originalHash).fold({ signatures ->
            val enoughSignaturesCollected = signCollector.isEnoughSignaturesCollected(tx, signatures)
            if (!enoughSignaturesCollected) {
                logger.info { "Not enough signatures were collected for tx $originalHash" }
                return
            }
            logger.info { "Tx $originalHash has enough signatures" }
            signCollector.fillTxWithSignatures(tx, signatures)
                .map {
                    unsignedTransactions.remove(shortTxHash)
                    logger.info { "Tx(originally known as $originalHash) is ready to be broadcasted $tx" }
                    broadcastTransactionListeners.forEach { listener ->
                        listener(tx)
                    }
                    //Wait until it is broadcasted to all connected peers
                    peerGroup.broadcastTransaction(tx).future().get()
                }.map {
                    onBroadcastSuccess()
                }.fold({
                    logger.info { "Tx ${tx.hashAsString} was successfully broadcasted" }
                    withdrawalStatistics.incSucceededTransfers()
                }, { ex ->
                    transactionHelper.unregisterUnspents(originalHash)
                    withdrawalStatistics.incFailedTransfers()
                    logger.error("Cannot complete tx $originalHash", ex)
                    btcRollbackService.rollback(
                        withdrawalCommand.sourceAccountId,
                        withdrawalCommand.amountSat,
                        withdrawalCommand.withdrawalTime,
                        "Cannot complete Bitcoin transaction"
                    )
                })

        }, { ex ->

            btcRollbackService.rollback(
                withdrawalCommand.sourceAccountId,
                withdrawalCommand.amountSat,
                withdrawalCommand.withdrawalTime,
                "Cannot get signatures for Bitcoin transaction"
            )
            withdrawalStatistics.incFailedTransfers()
            logger.error("Cannot get signatures for tx $originalHash", ex)
        })
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
