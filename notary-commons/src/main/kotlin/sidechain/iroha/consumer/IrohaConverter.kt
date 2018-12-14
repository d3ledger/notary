package sidechain.iroha.consumer

import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.TransactionBuilder
import notary.IrohaCommand
import notary.IrohaOrderedBatch
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

    /**
     * Converts batch into single Iroha transaction
     * @param batch - batch full of transactions
     * @param creator - transaction creator
     * @return single Iroha transaction
     */
    fun convert(batch: IrohaOrderedBatch, creator: String): List<Transaction> {
        val txList = mutableListOf<Transaction>()
        batch.transactions.forEach { tx ->
            val transaction = Transaction.builder(creator)
            tx.commands.forEach { command ->
                appendCommand(transaction, command)
            }
            txList.add(transaction.build())
        }
        return txList
    }
}
