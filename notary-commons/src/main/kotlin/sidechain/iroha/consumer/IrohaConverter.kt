package sidechain.iroha.consumer

import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.TransactionBuilder
import notary.IrohaCommand
import notary.IrohaOrderedBatch
import notary.IrohaTransaction
import javax.xml.bind.DatatypeConverter

/**
 * Class converts Notary [notary.IrohaOrderedBatch] to Iroha [UnsignedTx]
 */
object IrohaConverter {

    private fun appendCommand(txBuilder: TransactionBuilder, cmd: IrohaCommand): TransactionBuilder {
        return when (cmd) {
            is IrohaCommand.CommandCreateAccount ->
                txBuilder.createAccount(
                    cmd.accountName,
                    cmd.domainId,
                    DatatypeConverter.parseHexBinary(cmd.mainPubkey)
                )
            is IrohaCommand.CommandAddAssetQuantity ->
                txBuilder.addAssetQuantity(
                    cmd.assetId,
                    cmd.amount
                )
            is IrohaCommand.CommandAddSignatory ->
                txBuilder.addSignatory(
                    cmd.accountId,
                    DatatypeConverter.parseHexBinary(cmd.publicKey)
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
                    DatatypeConverter.parseHexBinary(cmd.peerKey)
                )
        }
    }

    private fun buildModelTransactionBuilder(transaction: IrohaTransaction): TransactionBuilder {
        return Transaction.builder(transaction.creator)
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
     * Converts batch into single Iroha transaction
     * @param batch - batch full of transactions
     * @return single Iroha transaction
     */
    fun convert(batch: IrohaOrderedBatch): List<Transaction> {
        val txList = mutableListOf<Transaction>()
        batch.transactions.forEach { tx ->
            txList.add(convert(tx))
        }
        return txList
    }
}
