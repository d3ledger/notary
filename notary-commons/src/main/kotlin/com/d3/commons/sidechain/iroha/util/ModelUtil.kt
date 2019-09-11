/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.util

import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.github.kittinunf.result.Result
import io.reactivex.Observable
import iroha.protocol.Primitive
import iroha.protocol.QryResponses
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.BlocksQueryBuilder
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import java.io.IOException
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyPair
import java.security.PublicKey
import java.time.Instant


object ModelUtil {
    val logger = KLogging().logger

    /**
     * Get current time
     * @return current time as bigint
     */
    fun getCurrentTime() = BigInteger.valueOf(System.currentTimeMillis())

    /**
     * Reads file from path as bytes
     * @param path to read bytes from
     * @return bytestring
     */
    fun readKeyFromFile(path: String): String? {
        return try {
            String(Files.readAllBytes(Paths.get(path)))
        } catch (ex: IOException) {
            logger.error("Unable to read key files", ex)
            null
        }
    }

    /**
     * Load keypair from existing files
     * @param pubkeyPath path to file with public key
     * @param privkeyPath path to file with private key
     */
    fun loadKeypair(pubkeyPath: String, privkeyPath: String): Result<KeyPair, Exception> {
        return Result.of {
            try {
                Utils.parseHexKeypair(readKeyFromFile(pubkeyPath), readKeyFromFile(privkeyPath))
            } catch (e: IOException) {
                throw Exception(
                    "Unable to read Iroha key files. Public key: $pubkeyPath, Private key: $privkeyPath",
                    e
                )
            }
        }
    }

    fun generateKeypair(): KeyPair {
        return Ed25519Sha3().generateKeypair()
    }

    /**
     * Send SetAccountDetail to Iroha
     * @param irohaConsumer - iroha network layer
     * @param accountId - account to set details
     * @param key - key of detail
     * @param value - value of detail
     * @param quorum - tx quorum. 1 by default
     * @return hex representation of transaction hash
     */
    fun setAccountDetail(
        irohaConsumer: IrohaConsumer,
        accountId: String,
        key: String,
        value: String,
        createdTime: Long = System.currentTimeMillis(),
        quorum: Int = 1
    ): Result<String, Exception> {
        val transactionBuilder = Transaction
            .builder(irohaConsumer.creator)
            .setAccountDetail(accountId, key, value)
            .setCreatedTime(createdTime)
            .setQuorum(quorum)
        return irohaConsumer.send(transactionBuilder.build())
    }

    /**
     * Send CreateAccount to Iroha
     * @param irohaConsumer - iroha network layer
     * @param name - account to be created
     * @param domain - domain for account
     * @param publicKey - public key of the account
     * @param roles - account roles
     * @return hex representation of transaction hash
     */
    fun createAccount(
        irohaConsumer: IrohaConsumer,
        name: String,
        domain: String,
        publicKey: PublicKey,
        roles: List<String>
    ): Result<String, Exception> {
        var txBuilder = Transaction
            .builder(irohaConsumer.creator)
            .createAccount(name, domain, publicKey)
        val accountId = "$name@$domain"
        roles.forEach { role ->
            txBuilder = txBuilder.appendRole(accountId, role)
        }
        val transaction = txBuilder
            .build()
        return irohaConsumer.send(transaction)
    }

    /**
     * Send GrantPermission to Iroha
     * @param irohaConsumer - iroha network layer
     * @param accountId - account to be granted
     * @param permissions - permissions to be granted
     * @return hex representation of transaction hash
     */
    fun grantPermissions(
        irohaConsumer: IrohaConsumer,
        accountId: String,
        permissions: Iterable<Primitive.GrantablePermission>
    ): Result<String, Exception> {
        val transaction = Transaction
            .builder(irohaConsumer.creator)
            .grantPermissions(accountId, permissions)
            .build()
        return irohaConsumer.send(transaction)
    }

    /**
     * Send AddSignatory to Iroha
     * @param irohaConsumer - iroha network layer
     * @param accountId - account to add key
     * @param publicKey - key to be added
     * @return hex representation of transaction hash
     */
    fun addSignatory(
        irohaConsumer: IrohaConsumer,
        accountId: String,
        publicKey: PublicKey
    ): Result<String, Exception> {
        val transaction = Transaction
            .builder(irohaConsumer.creator)
            .addSignatory(accountId, publicKey)
            .build()
        return irohaConsumer.send(transaction)
    }

    /**
     * Send SetAccountQourum to Iroha
     * @param irohaConsumer - iroha network layer
     * @param accountId - account to set quorum to
     * @param quorum - quorum to be set
     * @return hex representation of transaction hash
     */
    fun setAccountQuorum(
        irohaConsumer: IrohaConsumer,
        accountId: String,
        quorum: Int
    ): Result<String, Exception> {
        val transaction = Transaction
            .builder(irohaConsumer.creator)
            .setAccountQuorum(accountId, quorum)
            .build()
        return irohaConsumer.send(transaction)
    }

    /**
     * Send createAsset to Iroha
     * @param irohaConsumer - iroha network layer
     * @param assetName - asset name in iroha
     * @param domainId - domain id
     * @param precision - precision of asset
     * @return hex representation of transaction hash
     */
    fun createAsset(
        irohaConsumer: IrohaConsumer,
        assetName: String,
        domainId: String,
        precision: Int
    ): Result<String, Exception> {
        val transaction = Transaction
            .builder(irohaConsumer.creator)
            .createAsset(assetName, domainId, precision)
            .build()
        return irohaConsumer.send(transaction)
    }

    /**
     * Add asset in iroha
     * @param irohaConsumer - iroha network layer
     * @param assetId - asset name in Iroha
     * @param amount - amount to add
     * @return hex representation of transaction hash
     */
    fun addAssetIroha(
        irohaConsumer: IrohaConsumer,
        assetId: String,
        amount: String
    ): Result<String, Exception> {
        val transaction = Transaction
            .builder(irohaConsumer.creator)
            .addAssetQuantity(assetId, amount)
            .build()
        return irohaConsumer.send(transaction)
    }

    /**
     * Transfer asset in iroha
     * @param irohaConsumer - iroha network layer
     * @param srcAccountId - source account
     * @param destAccountId - destination account
     * @param assetId - asset id in Iroha
     * @param description - transfer description
     * @param amount - amount
     * @param creationTime - time of transaction creation. Current time by default.
     * @param quorum - tx quorum. 1 by default
     * @return hex representation of transaction hash
     */
    fun transferAssetIroha(
        irohaConsumer: IrohaConsumer,
        srcAccountId: String,
        destAccountId: String,
        assetId: String,
        description: String,
        amount: String,
        creationTime: Long = System.currentTimeMillis(),
        quorum: Int = 1
    ): Result<String, Exception> {
        val transaction = Transaction
            .builder(irohaConsumer.creator, creationTime)
            .transferAsset(srcAccountId, destAccountId, assetId, description, amount)
            .setQuorum(quorum)
            .build()
        return irohaConsumer.send(transaction)
    }

    /**
     * Get block streaming.
     */
    fun getBlockStreaming(
        iroha: IrohaAPI,
        credential: IrohaCredential
    ): Result<Observable<QryResponses.BlockQueryResponse>, Exception> {
        return Result.of {
            val query = BlocksQueryBuilder(credential.accountId, Instant.now(), 1)
                .buildSigned(credential.keyPair)

            iroha.blocksQuery(query)
        }
    }
}
