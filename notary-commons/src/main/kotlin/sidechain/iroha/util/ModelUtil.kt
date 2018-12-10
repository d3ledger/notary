package sidechain.iroha.util

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.google.protobuf.InvalidProtocolBufferException
import io.grpc.ManagedChannelBuilder
import iroha.protocol.CommandServiceGrpc
import iroha.protocol.QryResponses.QueryResponse
import iroha.protocol.Queries.BlocksQuery
import iroha.protocol.Queries.Query
import iroha.protocol.QueryServiceGrpc
import iroha.protocol.TransactionOuterClass.Transaction
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.*
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.consumer.IrohaNetwork
import java.io.IOException
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyPair
import java.security.PublicKey

object ModelUtil {
    val logger = KLogging().logger

    /**
     * Get iroha transaction builder
     * @return iroha transaction builder
     */
    fun getModelTransactionBuilder() = ModelTransactionBuilder()

    /**
     * Get iroha query builder
     * @return iroha query builder
     */
    fun getModelQueryBuilder() = ModelQueryBuilder()

    /**
     * Get current time
     * @return current time as bigint
     */
    fun getCurrentTime() = BigInteger.valueOf(System.currentTimeMillis())

    /**
     * Opens a chanel with iroha peer
     * @param host - the address, either ip or hostname
     * @port to connect
     * @return managed channel
     */
    fun getChannel(host: String, port: Int) = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()

    /**
     * Creates a stub for commands
     * @param channel - channel with Iroha peer
     * @return torii command stub
     */
    fun getCommandStub(channel: io.grpc.Channel): CommandServiceGrpc.CommandServiceBlockingStub {
        return CommandServiceGrpc.newBlockingStub(channel)
    }

    /**
     * Creates a stub for queries
     * @param channel - channel with Iroha peer
     */
    fun getQueryStub(channel: io.grpc.Channel): QueryServiceGrpc.QueryServiceBlockingStub {
        return QueryServiceGrpc.newBlockingStub(channel)
    }

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

    // TODO temprorary function, should be removed after move to Iroha Pure Java Lib
    // is used to convert iroha keys from Iroha Bindings lib representation to Iroha Pure Java lib
    private fun hexToByte(hexString: String): Byte {
        val firstDigit = toDigit(hexString[0])
        val secondDigit = toDigit(hexString[1])
        return ((firstDigit shl 4) + secondDigit).toByte()
    }

    // TODO temprorary function, should be removed after move to Iroha Pure Java Lib
    // is used to convert iroha keys from Iroha Bindings lib representation to Iroha Pure Java lib
    private fun toDigit(hexChar: Char): Int {
        val digit = Character.digit(hexChar, 16)
        if (digit == -1) {
            throw IllegalArgumentException(
                "Invalid Hexadecimal Character: $hexChar"
            )
        }
        return digit
    }

    // TODO temprorary function, should be removed after move to Iroha Pure Java Lib
    // is used to convert iroha keys from Iroha Bindings lib representation to Iroha Pure Java lib
    private fun decodeHexString(hexString: String): ByteArray {
        if (hexString.length % 2 == 1) {
            throw IllegalArgumentException(
                "Invalid hexadecimal String supplied."
            )
        }

        val bytes = ByteArray(hexString.length / 2)
        var i = 0
        while (i < hexString.length) {
            bytes[i / 2] = hexToByte(hexString.substring(i, i + 2))
            i += 2
        }
        return bytes
    }

    // TODO temprorary function, should be removed after move to Iroha Pure Java Lib
    // is used to convert iroha keys from Iroha Bindings lib representation to Iroha Pure Java lib
    private fun bytesToHex(bytes: ByteArray): String {
        var str = ""
        for (b in bytes) {
            str = str + String.format("%02X", b)
        }

        return str
    }

    /**
     * Load keypair from existing files
     * @param pubkeyPath path to file with public key
     * @param privkeyPath path to file with private key
     */
    fun loadKeypair(pubkeyPath: String, privkeyPath: String): Result<KeyPair, Exception> {
        return Result.of {
            try {
                Ed25519Sha3.keyPairFromBytes(
                    decodeHexString(readKeyFromFile(privkeyPath)!!),
                    decodeHexString(readKeyFromFile(pubkeyPath)!!)
                )
            } catch (e: IOException) {
                throw Exception("Unable to read Iroha key files. Public key: $pubkeyPath, Private key: $privkeyPath", e)
            }
        }
    }

    fun generateKeypair(): KeyPair {
        return Ed25519Sha3().generateKeypair()
    }

    /**
     * Prepares query before sending it to a peer
     * @param uquery - unsigned model query
     * @param keys used to sign query
     * @return proto query, if succeeded
     */
    fun prepareQuery(uquery: UnsignedQuery, keys: KeyPair): Result<Query, Exception> {
        val modelKeys =
            ModelCrypto().convertFromExisting(bytesToHex(keys.public.encoded), bytesToHex(keys.private.encoded))

        return Result.of {
            val bquery = ModelProtoQuery(uquery)
                .signAndAddSignature(modelKeys)
                .finish()
                .blob()
                .toByteArray()
            Query.parseFrom(bquery)
        }
    }

    /**
     * Prepares query for blocks before sending it to a peer
     * @param uquery - unsigned model blocks query
     * @param keys used to sign blocks query
     * @return proto blocks query, if succeeded
     */
    fun prepareBlocksQuery(uquery: UnsignedBlockQuery, keys: KeyPair): BlocksQuery? {
        val modelKeys =
            ModelCrypto().convertFromExisting(bytesToHex(keys.public.encoded), bytesToHex(keys.private.encoded))

        val queryBlob = ModelProtoBlocksQuery(uquery)
            .signAndAddSignature(modelKeys)
            .finish()
            .blob()
        val bquery = queryBlob.toByteArray()

        var protoQuery: BlocksQuery? = null
        try {
            protoQuery = BlocksQuery.parseFrom(bquery)
        } catch (e: InvalidProtocolBufferException) {
            logger.error("Exception while converting byte array to protobuf", e)
        }
        return protoQuery
    }

    /**
     * Prepares transaction before sending it to a peer
     * @param utx - unsigned model transaction
     * @param keys used to sign transaction
     * @return proto transaction, if succeeded
     */
    fun prepareTransaction(utx: UnsignedTx, keys: KeyPair): Result<Transaction, Exception> {
        val modelKeys =
            ModelCrypto().convertFromExisting(bytesToHex(keys.public.encoded), bytesToHex(keys.private.encoded))

        return Result.of {
            // sign transaction and get its binary representation (Blob)
            val tx = ModelProtoTransaction(utx)
                .signAndAddSignature(modelKeys)
                .finish()
            val blob = tx.blob()

            // Convert ByteVector to byte array
            val bs = blob.toByteArray()
            val protoTx = Transaction.parseFrom(bs)
            protoTx
        }
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
     * @param hash - hash of transaction
     * @return transaction
     */
    fun getTransaction(
        irohaNetwork: IrohaNetwork,
        credential: IrohaCredential,
        hash: String
    ): Result<Transaction, Exception> {
        val hashes = HashVector()
        hashes.add(Hash.fromHexString(hash))

        val uquery = ModelQueryBuilder().creatorAccountId(credential.accountId)
            .queryCounter(BigInteger.valueOf(1))
            .createdTime(BigInteger.valueOf(System.currentTimeMillis()))
            .getTransactions(hashes)
            .build()

        return prepareQuery(uquery, credential.keyPair)
            .flatMap { irohaNetwork.sendQuery(it) }
            .flatMap { getFirstTransaction(it) }
    }

    /**
     * Send SetAccountDetail to Iroha
     * @param irohaConsumer - iroha network layer
     * @param accountId - account to set details
     * @param key - key of detail
     * @param value - value of detail
     * @param createdTime - time of tx creation. Current time by default
     * @return hex representation of transaction hash
     */
    fun setAccountDetail(
        irohaConsumer: IrohaConsumer,
        accountId: String,
        key: String,
        value: String,
        createdTime: BigInteger = getCurrentTime()
    ): Result<String, Exception> {
        val tx = ModelTransactionBuilder()
            .creatorAccountId(irohaConsumer.creator)
            .createdTime(createdTime)
            .setAccountDetail(accountId, key, value)
            .build()
        return irohaConsumer.sendAndCheck(tx)
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
    ): Result<String, Exception> {
        return Result.of {
            val oldPubkey = PublicKey(jp.co.soramitsu.iroha.PublicKey.fromHexString(bytesToHex(publicKey.encoded)))

            var txBuilder = ModelTransactionBuilder()
                .creatorAccountId(irohaConsumer.creator)
                .createdTime(getCurrentTime())
                .createAccount(name, domain, oldPubkey)

            roleName.forEach {
                txBuilder = txBuilder.appendRole("$name@$domain", it)
            }

            txBuilder.build()
        }.flatMap { utx ->
            irohaConsumer.sendAndCheck(utx)
        }
    }

    /**
     * Send GrantPermission to Iroha
     * @param irohaConsumer - iroha network layer
     * @param accountId - account to be granted
     * @param permissions - permissions to be granted
     * @return hex representation of transaction hash
     */
    fun grantPermission(
        irohaConsumer: IrohaConsumer,
        accountId: String,
        vararg permissions: Grantable
    ): Result<String, Exception> {
        return Result.of {
            var txBuilder = ModelTransactionBuilder()
                .creatorAccountId(irohaConsumer.creator)
                .createdTime(getCurrentTime())

            permissions.forEach { permission ->
                txBuilder = txBuilder.grantPermission(accountId, permission)
            }

            txBuilder.build()
        }.flatMap { utx ->
            irohaConsumer.sendAndCheck(utx)
        }
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
        return Result.of {
            val oldPubkey = PublicKey(jp.co.soramitsu.iroha.PublicKey.fromHexString(bytesToHex(publicKey.encoded)))

            ModelTransactionBuilder()
                .creatorAccountId(irohaConsumer.creator)
                .createdTime(getCurrentTime())
                .addSignatory(accountId, oldPubkey)
                .build()
        }.flatMap { utx ->
            irohaConsumer.sendAndCheck(utx)
        }
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
        return Result.of {
            ModelTransactionBuilder()
                .creatorAccountId(irohaConsumer.creator)
                .createdTime(getCurrentTime())
                .setAccountQuorum(accountId, quorum)
                .build()
        }.flatMap { utx ->
            irohaConsumer.sendAndCheck(utx)
        }
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
        precision: Short
    ): Result<String, Exception> {
        return irohaConsumer.sendAndCheck(
            ModelTransactionBuilder()
                .creatorAccountId(irohaConsumer.creator)
                .createdTime(getCurrentTime())
                .createAsset(assetName, domainId, precision)
                .build()
        )
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
        return irohaConsumer.sendAndCheck(
            ModelTransactionBuilder()
                .creatorAccountId(irohaConsumer.creator)
                .createdTime(getCurrentTime())
                .addAssetQuantity(assetId, amount)
                .build()
        )
    }

    /**
     * Transfer asset in iroha
     * @param irohaConsumer - iroha network layer
     * @param srcAccountId - source account
     * @param destAccountId - destination account
     * @param assetId - asset id in Iroha
     * @param description - transfer description
     * @param amount - amount
     * @param createdTime - time of transaction creation. Current time by default.
     * @return hex representation of transaction hash
     */
    fun transferAssetIroha(
        irohaConsumer: IrohaConsumer,
        srcAccountId: String,
        destAccountId: String,
        assetId: String,
        description: String,
        amount: String,
        createdTime: BigInteger = getCurrentTime()
    ): Result<String, Exception> {
        return irohaConsumer.sendAndCheck(
            ModelTransactionBuilder()
                .creatorAccountId(irohaConsumer.creator)
                .createdTime(createdTime)
                .transferAsset(srcAccountId, destAccountId, assetId, description, amount)
                .build()
        )
    }

    /**
     * Create account in Iroha.
     * @param name - account name
     * @param domain - account domain
     * @return hex
     */
    fun createAccount(
        irohaConsumer: IrohaConsumer,
        name: String,
        domain: String,
        pubkey: PublicKey
    ): Result<String, Exception> {
        return irohaConsumer.sendAndCheck(
            ModelTransactionBuilder()
                .creatorAccountId(irohaConsumer.creator)
                .createdTime(getCurrentTime())
                .createAccount(name, domain, pubkey)
                .build()
        )
    }
}
