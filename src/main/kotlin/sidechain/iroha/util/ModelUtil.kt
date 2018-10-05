package sidechain.iroha.util

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import com.google.protobuf.InvalidProtocolBufferException
import io.grpc.ManagedChannelBuilder
import iroha.protocol.CommandServiceGrpc
import iroha.protocol.QryResponses.QueryResponse
import iroha.protocol.Queries.BlocksQuery
import iroha.protocol.Queries.Query
import iroha.protocol.QueryServiceGrpc
import iroha.protocol.TransactionOuterClass.Transaction
import jp.co.soramitsu.iroha.*
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.consumer.IrohaNetwork
import token.EthTokenInfo
import java.io.IOException
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths

object ModelUtil {
    val logger = KLogging().logger

    /**
     * Get iroha crypto library
     * @return cryptograpty module
     */
    fun getModelCrypto() = ModelCrypto()

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
    fun getChannel(host: String, port: Int) = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build()

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

    /**
     * Gets keys for a concrete user
     * @param path to get keys from
     * @param user which keys to read
     * @return user's keypair
     */
    fun getKeys(path: String, user: String) = getModelCrypto().convertFromExisting(
        readKeyFromFile("$path/$user.pub"),
        readKeyFromFile("$path/$user.priv")
    )

    /**
     * Load keypair from existing files
     * @param pubkeyPath path to file with public key
     * @param privkeyPath path to file with private key
     */
    fun loadKeypair(pubkeyPath: String, privkeyPath: String): Result<Keypair, Exception> {
        val crypto = ModelCrypto()
        return Result.of {
            try {
                crypto.convertFromExisting(
                    String(java.nio.file.Files.readAllBytes(Paths.get(pubkeyPath))),
                    String(java.nio.file.Files.readAllBytes(Paths.get(privkeyPath)))
                )
            } catch (e: IOException) {
                throw Exception("Unable to read Iroha key files. Public key: $pubkeyPath, Private key: $privkeyPath", e)
            }
        }
    }

    /**
     * Prepares query before sending it to a peer
     * @param uquery - unsigned model query
     * @param keys used to sign query
     * @return proto query, if succeeded
     */
    fun prepareQuery(uquery: UnsignedQuery, keys: Keypair): Result<Query, Exception> {
        return Result.of {
            val bquery = ModelProtoQuery(uquery)
                .signAndAddSignature(keys)
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
    fun prepareBlocksQuery(uquery: UnsignedBlockQuery, keys: Keypair): BlocksQuery? {

        val queryBlob = ModelProtoBlocksQuery(uquery)
            .signAndAddSignature(keys)
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
    fun prepareTransaction(utx: UnsignedTx, keys: Keypair): Result<Transaction, Exception> {
        return Result.of {
            // sign transaction and get its binary representation (Blob)
            val tx = ModelProtoTransaction(utx)
                .signAndAddSignature(keys)
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
     * @return hex representation of transaction hash
     */
    fun setAccountDetail(
        irohaConsumer: IrohaConsumer,
        accountId: String,
        key: String,
        value: String
    ): Result<String, Exception> {
        val tx = ModelTransactionBuilder()
            .creatorAccountId(irohaConsumer.creator)
            .createdTime(getCurrentTime())
            .setAccountDetail(accountId, key, value)
            .build()
        return irohaConsumer.sendAndCheck(tx)
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
     * @return hex representation of transaction hash
     */
    fun transferAssetIroha(
        irohaConsumer: IrohaConsumer,
        srcAccountId: String,
        destAccountId: String,
        assetId: String,
        description: String,
        amount: String
    ): Result<String, Exception> {
        return irohaConsumer.sendAndCheck(
            ModelTransactionBuilder()
                .creatorAccountId(irohaConsumer.creator)
                .createdTime(getCurrentTime())
                .transferAsset(srcAccountId, destAccountId, assetId, description, amount)
                .build()
        )
    }

    /**
     * Registers ERC20 tokens in Iroha
     * @param tokens - map of tokens to register(address->token info
     * @param tokenStorageAccount - account that holds tokens
     * @param irohaConsumer - iroha network layer
     * @return hex representation of transaction hash
     */
    fun registerERC20Tokens(
        tokens: Map<String, EthTokenInfo>,
        tokenStorageAccount: String,
        irohaConsumer: IrohaConsumer
    ): Result<String, Exception> {
        return Result.of {
            var utx = ModelTransactionBuilder()
                .creatorAccountId(irohaConsumer.creator)
                .createdTime(BigInteger.valueOf(System.currentTimeMillis()))
            tokens.forEach { ethWallet, ethTokenInfo ->
                utx = utx.createAsset(ethTokenInfo.name, "ethereum", ethTokenInfo.precision)
                utx = utx.setAccountDetail(tokenStorageAccount, ethWallet, ethTokenInfo.name)
            }

            utx.build()
        }.flatMap { utx ->
            irohaConsumer.sendAndCheck(utx)
        }
    }

    /**
     * Create new Iroha account with [accountName].
     */
    fun createAccount(
        irohaConsumer: IrohaConsumer,
        irohaKeyPair: Keypair,
        accountName: String
    ): Result<String, Exception> {
        val domain = "notary"
        return irohaConsumer.sendAndCheck(
            ModelTransactionBuilder()
                .creatorAccountId(irohaConsumer.creator)
                .createdTime(ModelUtil.getCurrentTime())
                .createAccount(accountName, domain, irohaKeyPair.publicKey())
                .build()
        ).map { "$accountName@$domain" }
    }
}
