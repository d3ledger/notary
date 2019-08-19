/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.consumer

import com.d3.commons.notary.IrohaCommand
import com.d3.commons.notary.IrohaOrderedBatch
import com.d3.commons.notary.IrohaTransaction
import com.d3.commons.util.unHex
import iroha.protocol.Primitive
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.TransactionBuilder
import jp.co.soramitsu.iroha.java.Utils
import java.security.KeyPair

/**
 * Class converts Notary [com.d3.commons.notary.IrohaOrderedBatch] to Iroha [Transaction]
 */
object IrohaConverter {

    private fun appendCommand(
        txBuilder: TransactionBuilder,
        cmd: IrohaCommand
    ): TransactionBuilder {
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
            is IrohaCommand.CommandGrantPermission ->
                txBuilder.grantPermission(
                    cmd.accountId,
                    Primitive.GrantablePermission.forNumber(cmd.permission)
                )
            is IrohaCommand.CommandSetAccountQuorum ->
                txBuilder.setAccountQuorum(
                    cmd.accountId,
                    cmd.quorum
                )
        }
    }

    private fun buildModelTransactionBuilder(transaction: IrohaTransaction): TransactionBuilder {
        val builder = Transaction.builder(transaction.creator, transaction.createdTime.toLong())
        return if (transaction.quorum == null) builder
        else builder.setQuorum(transaction.quorum)
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
     * Convert Notary intention [com.d3.commons.notary.IrohaTransaction] to unsigned Iroha java [Transaction]
     * @param transaction - Iroha transaction to convert
     * @return unsigned transaction
     */
    fun convert(transaction: IrohaTransaction): Transaction {
        val txBuilder = appendCommands(buildModelTransactionBuilder(transaction), transaction)
        return txBuilder.build()
    }

    /**
     * Convert Notary intention [com.d3.commons.notary.IrohaTransaction] to Iroha java [Transaction]
     * @param transaction - Iroha transaction to convert
     * @param keyPair - keypair that is used to sign given transaction
     * @return signed transaction
     */
    fun convert(transaction: IrohaTransaction, keyPair: KeyPair): Transaction {
        val txBuilder = appendCommands(buildModelTransactionBuilder(transaction), transaction)
        txBuilder.sign(keyPair)
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

    /**
     * Converts batch into Iroha protobuf transactions batch list
     * @param batch - batch full of transactions
     * @return IPJ unsigned transactions batch list
     */
    fun convertToUnsignedBatch(batch: IrohaOrderedBatch): List<Transaction> {
        return Utils.createTxUnsignedAtomicBatch(convert(batch)).toList()
    }
}
