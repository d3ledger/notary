package sidechain.iroha.consumer

import jp.co.soramitsu.iroha.ModelTransactionBuilder
import jp.co.soramitsu.iroha.PublicKey
import jp.co.soramitsu.iroha.UnsignedTx
import mu.KLogging
import notary.IrohaCommand
import notary.IrohaOrderedBatch
import java.math.BigInteger

/**
 * Class converts Notary [notary.IrohaOrderedBatch] to Iroha [UnsignedTx]
 */
class IrohaConverterImpl {

    /**
     * Convert Notary [notary.IrohaOrderedBatch] to Iroha [UnsignedTx]
     */
    fun convert(batch: IrohaOrderedBatch): List<UnsignedTx> {
        // TODO rework with batch transactions
        val txs = mutableListOf<UnsignedTx>()

        for (transaction in batch.transactions) {
            var txBuilder = ModelTransactionBuilder()
                .creatorAccountId(transaction.creator)
                .createdTime(BigInteger.valueOf(System.currentTimeMillis()))

            for (cmd in transaction.commands) {
                when (cmd) {
                    is IrohaCommand.CommandAddAssetQuantity ->
                        txBuilder = txBuilder.addAssetQuantity(
                            cmd.accountId,
                            cmd.assetId,
                            cmd.amount
                        )
                    is IrohaCommand.CommandAddSignatory ->
                        txBuilder = txBuilder.addSignatory(
                            cmd.accountId,
                            PublicKey(cmd.publicKey)
                        )
                    is IrohaCommand.CommandCreateAsset ->
                        txBuilder = txBuilder.createAsset(
                            cmd.assetName,
                            cmd.domainId,
                            cmd.precision
                        )
                    is IrohaCommand.CommandSetAccountDetail ->
                        txBuilder = txBuilder.setAccountDetail(
                            cmd.accountId,
                            cmd.key,
                            cmd.value
                        )
                    is IrohaCommand.CommandTransferAsset ->
                        txBuilder = txBuilder.transferAsset(
                            cmd.srcAccountId,
                            cmd.destAccountId,
                            cmd.assetId,
                            cmd.description,
                            cmd.amount
                        )
                }
            }
            txs.add(txBuilder.build())
        }
        return txs
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
