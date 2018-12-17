package sidechain.iroha.util

import com.github.kittinunf.result.Result
import io.reactivex.Observable
import iroha.protocol.Primitive
import iroha.protocol.QryResponses
import iroha.protocol.QryResponses.QueryResponse
import iroha.protocol.TransactionOuterClass.Transaction
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.BlocksQueryBuilder
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Query
import jp.co.soramitsu.iroha.java.Utils
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.consumer.IrohaConsumer
import util.unHex
import java.io.IOException
import java.math.BigDecimal
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
                Utils.keyPair(readKeyFromFile(pubkeyPath), readKeyFromFile(privkeyPath))
            } catch (e: IOException) {
                throw Exception("Unable to read Iroha key files. Public key: $pubkeyPath, Private key: $privkeyPath", e)
            }
        }
    }

    fun generateKeypair(): KeyPair {
        return Ed25519Sha3().generateKeypair()
    }

    /**
     * Checks if query response is stateless valid, used to
     * know when to increase query counter
     * @param resp - query response
     * @return true if query is stateless valid
     */
    fun isStatelessValid(resp: QueryResponse) =
        !(resp.hasErrorResponse() &&
                resp.errorResponse.reason.toString() == "STATELESS_INVALID")

    /**
     * Get transaction from Iroha by [hash]
     * @param iroha - iroha network layer
     * @param irohaCredential - iroha account
     * @param hash - hash of transaction
     * @return transaction
     */
    fun getTransaction(
        iroha: IrohaAPI,
        irohaCredential: IrohaCredential,
        hash: String
    ): Result<Transaction, Exception> {
        val query = Query.builder(irohaCredential.accountId, 1)
            .getTransactions(listOf(String.unHex(hash)))
            .buildSigned(irohaCredential.keyPair)
        return getFirstTransaction(iroha.query(query))
    }

    /**
     * Send SetAccountDetail to Iroha
     * @param irohaConsumer - iroha network layer
     * @param accountId - account to set details
     * @param key - key of detail
     * @param value - value of detail
     * @return hex representation of transaction hash
     */
    fun setAccountDetail(
        irohaConsumer: IrohaConsumer,
        accountId: String,
        key: String,
        value: String
    ): Result<ByteArray, Exception> {
        val transaction = jp.co.soramitsu.iroha.java.Transaction
            .builder(irohaConsumer.creator)
            .setAccountDetail(accountId, key, value)
            .build()
        return irohaConsumer.send(transaction)
    }

    /**
     * Send CreateAccount to Iroha
     * @param irohaConsumer - iroha network layer
     * @param name - account to be created
     * @param domain - domain for account
     * @param publicKey - public key of the account
     * @return hex representation of transaction hash
     */
    fun createAccount(
        irohaConsumer: IrohaConsumer,
        name: String,
        domain: String,
        publicKey: PublicKey,
        vararg roleName: String
    ): Result<ByteArray, Exception> {
        var txBuilder = jp.co.soramitsu.iroha.java.Transaction
            .builder(irohaConsumer.creator)
            .createAccount(name, domain, publicKey)
        val accountId = "$name@$domain"
        roleName.forEach { role ->
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
    ): Result<ByteArray, Exception> {
        val transaction = jp.co.soramitsu.iroha.java.Transaction
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
    ): Result<ByteArray, Exception> {
        val transaction = jp.co.soramitsu.iroha.java.Transaction
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
    ): Result<ByteArray, Exception> {
        val transaction = jp.co.soramitsu.iroha.java.Transaction
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
    ): Result<ByteArray, Exception> {
        val transaction = jp.co.soramitsu.iroha.java.Transaction
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
        amount: BigDecimal
    ): Result<ByteArray, Exception> {
        val transaction = jp.co.soramitsu.iroha.java.Transaction
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
     * @return hex representation of transaction hash
     */
    fun transferAssetIroha(
        irohaConsumer: IrohaConsumer,
        srcAccountId: String,
        destAccountId: String,
        assetId: String,
        description: String,
        amount: String
    ): Result<ByteArray, Exception> {
        val transaction = jp.co.soramitsu.iroha.java.Transaction
            .builder(irohaConsumer.creator)
            .transferAsset(srcAccountId, destAccountId, assetId, description, amount)
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
