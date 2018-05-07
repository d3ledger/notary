package sideChain.iroha.consumer

import ModelTransactionBuilder
import PublicKey
import UnsignedTx
import notary.IrohaCommand
import notary.IrohaTransaction
import java.math.BigInteger

/**
 * Class converts Notary [notary.IrohaTransaction] to Iroha [UnsignedTx]
 */
class IrohaConverterImpl {

    /**
     * Convert Notary [notary.IrohaTransaction] to Iroha [UnsignedTx]
     */
    fun convert(transaction: IrohaTransaction): UnsignedTx {
        var txBuilder = ModelTransactionBuilder()
            .creatorAccountId(transaction.creator)
            .createdTime(BigInteger.valueOf(System.currentTimeMillis()))
            .txCounter(BigInteger.valueOf(1))

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
        return txBuilder.build()
    }

}
