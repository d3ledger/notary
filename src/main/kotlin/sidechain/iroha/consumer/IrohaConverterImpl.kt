package sidechain.iroha.consumer

import jp.co.soramitsu.iroha.ModelTransactionBuilder
import jp.co.soramitsu.iroha.PublicKey
import jp.co.soramitsu.iroha.UnsignedTx
import mu.KLogging
import notary.IrohaCommand
import notary.IrohaOrderedBatch
import notary.IrohaTransaction
import java.math.BigInteger

/**
 * Class converts Notary [notary.IrohaOrderedBatch] to Iroha [UnsignedTx]
 */
class IrohaConverterImpl {

    /**
     * Convert Notary intention [notary.IrohaTransaction] to Iroha protobuf [UnsignedTx]
     */
    fun convert(transaction: IrohaTransaction): UnsignedTx {
        var txBuilder = ModelTransactionBuilder()
            .creatorAccountId(transaction.creator)
            .createdTime(BigInteger.valueOf(System.currentTimeMillis()))

        for (cmd in transaction.commands) {
            when (cmd) {
                is IrohaCommand.CommandCreateAccount ->
                    txBuilder = txBuilder.createAccount(
                        cmd.accountName,
                        cmd.domainId,
                        PublicKey(PublicKey.fromHexString(cmd.mainPubkey))
                    )
                is IrohaCommand.CommandAddAssetQuantity ->
                    txBuilder = txBuilder.addAssetQuantity(
                        cmd.accountId,
                        cmd.assetId,
                        cmd.amount
                    )
                is IrohaCommand.CommandAddSignatory ->
                    txBuilder = txBuilder.addSignatory(
                        cmd.accountId,
                        PublicKey(PublicKey.fromHexString(cmd.publicKey))
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

        return txBuilder.build()
    }

    /**
     * Convert Notary [notary.IrohaOrderedBatch] to Iroha [UnsignedTx]
     */
    fun convert(batch: IrohaOrderedBatch): List<UnsignedTx> {
        // TODO rework with batch transactions
        val txs = mutableListOf<UnsignedTx>()

        for (transaction in batch.transactions) {
            txs.add(convert(transaction))
        }
        return txs
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
