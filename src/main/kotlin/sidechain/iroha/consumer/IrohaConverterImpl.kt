package sidechain.iroha.consumer

import jp.co.soramitsu.iroha.*
import mu.KLogging
import notary.IrohaCommand
import notary.IrohaOrderedBatch
import notary.IrohaTransaction
import java.math.BigInteger

/**
 * Class converts Notary [notary.IrohaOrderedBatch] to Iroha [UnsignedTx]
 */
class IrohaConverterImpl {

    private fun appendCommand(txBuilder: ModelTransactionBuilder, cmd: IrohaCommand): ModelTransactionBuilder {
        return when (cmd) {
            is IrohaCommand.CommandCreateAccount ->
                txBuilder.createAccount(
                    cmd.accountName,
                    cmd.domainId,
                    PublicKey(PublicKey.fromHexString(cmd.mainPubkey))
                )
            is IrohaCommand.CommandAddAssetQuantity ->
                txBuilder.addAssetQuantity(
                    cmd.assetId,
                    cmd.amount
                )
            is IrohaCommand.CommandAddSignatory ->
                txBuilder.addSignatory(
                    cmd.accountId,
                    PublicKey(PublicKey.fromHexString(cmd.publicKey))
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
                    PublicKey(PublicKey.fromHexString(cmd.peerKey))
                )
        }
    }

    private fun buildModelTransactionBuilder(transaction: IrohaTransaction): ModelTransactionBuilder {
        return ModelTransactionBuilder()
            .creatorAccountId(transaction.creator)
            .createdTime(transaction.createdTime)
            .quorum(transaction.quorum)
    }

    private fun appendCommands(
        txBuilder: ModelTransactionBuilder,
        transaction: IrohaTransaction
    ): ModelTransactionBuilder {
        var builder = txBuilder
        transaction.commands.forEach { cmd ->
            builder = appendCommand(builder, cmd)
        }
        return builder
    }

    /**
     * Convert Notary intention [notary.IrohaTransaction] to Iroha protobuf [UnsignedTx]
     */
    fun convert(transaction: IrohaTransaction): UnsignedTx {
        val txBuilder = appendCommands(buildModelTransactionBuilder(transaction), transaction)
        return txBuilder.build()
    }

    /**
     * Convert Notary [notary.IrohaOrderedBatch] to Iroha [UnsignedTx]
     */
    fun convert(batch: IrohaOrderedBatch): List<UnsignedTx> {
        val txs = mutableListOf<UnsignedTx>()

        val hashes = HashVector()
        batch.transactions.map {
            val utx = convert(it)
            iroha.utxReducedHash(utx)
        }
            .forEach { hashes.add(it) }

        batch.transactions.forEach { tx ->
            val txBuilder = appendCommands(buildModelTransactionBuilder(tx), tx)
                .batchMeta(BatchType.ORDERED, hashes)

            txs.add(txBuilder.build())

        }
        return txs
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
