package sidechain.iroha.consumer

import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.TransactionBuilder
import jp.co.soramitsu.iroha.java.Utils
import notary.IrohaOrderedBatch
import notary.IrohaCommand
import notary.IrohaTransaction
import util.unHex
import java.security.KeyPair

/**
 * Class converts Notary [notary.IrohaOrderedBatch] to Iroha [Transaction]
 */
object IrohaConverter {

    private fun appendCommand(txBuilder: TransactionBuilder, cmd: IrohaCommand): TransactionBuilder {
        return when (cmd) {
            is IrohaCommand.CommandCreateAccount ->
                txBuilder.createAccount(
                    cmd.accountName,
                    cmd.domainId,
                    String.unHex(cmd.mainPubkey)
                )
            is IrohaCommand.CommandAddAssetQuantity ->
                txBuilder.addAssetQuantity(
                    cmd.assetId,
                    cmd.amount
                )
            is IrohaCommand.CommandAddSignatory ->
                txBuilder.addSignatory(
                    cmd.accountId,
                    String.unHex(cmd.publicKey)
                )
            is IrohaCommand.CommandCreateAsset ->
                txBuilder.createAsset(
                    cmd.assetName,
                    cmd.domainId,
                    cmd.precision
                )
            is IrohaCommand.CommandSetAccountDetail ->
                txBuilder.setAccountDetail(
                    cmd.accountId,
                    cmd.key,
                    cmd.value
                )
            is IrohaCommand.CommandTransferAsset ->
                txBuilder.transferAsset(
                    cmd.srcAccountId,
                    cmd.destAccountId,
                    cmd.assetId,
                    cmd.description,
                    cmd.amount
                )
            is IrohaCommand.CommandAddPeer ->
                txBuilder.addPeer(
                    cmd.address,
                    String.unHex(cmd.peerKey)
                )
        }
    }

    private fun buildModelTransactionBuilder(transaction: IrohaTransaction): TransactionBuilder {
        return Transaction.builder(transaction.creator, transaction.createdTime.toLong())
            .setQuorum(transaction.quorum)
    }

    private fun appendCommands(
        txBuilder: TransactionBuilder,
        transaction: IrohaTransaction
    ): TransactionBuilder {
        var builder = txBuilder
        transaction.commands.forEach { cmd ->
            builder = appendCommand(builder, cmd)
        }
        return builder
    }

    /**
     * Convert Notary intention [notary.IrohaTransaction] to Iroha java [Transaction]
     */
    fun convert(transaction: IrohaTransaction): Transaction {
        val txBuilder = appendCommands(buildModelTransactionBuilder(transaction), transaction)
        return txBuilder.build()
    }

    /**
     * Converts batch into IPJ transactions list (JUST LIST, NOT BATCH)
     * @param batch - batch full of transactions
     * @return IPJ transactions list
     */
    fun convert(batch: IrohaOrderedBatch): List<Transaction> {
        return batch.transactions.map { convert(it) }
    }

    /**
     * Converts batch into Iroha protobuf transactions batch list
     * @param batch - batch full of transactions
     * @param keyPair - credential to sign transactions
     * @return Iroha protobuf transactions batch list
     */
    fun convert(
        batch: IrohaOrderedBatch,
        keyPair: KeyPair
    ): Iterable<TransactionOuterClass.Transaction> {
        return Utils.createTxOrderedBatch(convert(batch).map { it.build() }, keyPair)
    }
}
