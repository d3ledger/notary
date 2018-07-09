package sidechain.iroha.util


import com.github.kittinunf.result.Result
import com.google.protobuf.InvalidProtocolBufferException
import com.squareup.moshi.JsonReader
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import iroha.protocol.BlockOuterClass.Transaction
import iroha.protocol.CommandServiceGrpc
import iroha.protocol.Queries.BlocksQuery
import iroha.protocol.Queries.Query
import iroha.protocol.QueryServiceGrpc
import iroha.protocol.Responses.QueryResponse
import jp.co.soramitsu.iroha.*
import mu.KLogging
import okio.Okio
import java.io.ByteArrayInputStream
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
    fun getChannel(host: String = "localhost", ip: Int = 50051) =
        ManagedChannelBuilder.forAddress(host, ip).usePlaintext(true).build()

    /**
     * Creates a stub for commands
     * @param channel to communicate through
     * @return torii command stub
     */
    fun getCommandStub(
        channel: ManagedChannel = getChannel()
    ): CommandServiceGrpc.CommandServiceBlockingStub =
        CommandServiceGrpc.newBlockingStub(channel)

    /**
     * Creates a stub for queries
     * @param channel to communicate through
     * @return torii query stub
     */
    fun getQueryStub(
        channel: ManagedChannel = getChannel()
    ): QueryServiceGrpc.QueryServiceBlockingStub =
        QueryServiceGrpc.newBlockingStub(channel)


    /**
     * Reads file from path as bytes
     * @param path to read bytes from
     * @return bytestring
     */
    fun readKeyFromFile(path: String): String? {
        return try {
            String(Files.readAllBytes(Paths.get(path)))
        } catch (e: IOException) {
            logger.error { "Unable to read key files.\n $e" }
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
                throw Exception("Unable to read Iroha key files. \n ${e.message}", e)
            }
        }
    }

    /**
     * Prepares query before sending it to a peer
     * @param uquery - unsigned model query
     * @param keys used to sign query
     * @return proto query, if succeeded
     */
    fun prepareQuery(uquery: UnsignedQuery, keys: Keypair): Query? {

        val queryBlob = ModelProtoQuery(uquery)
            .signAndAddSignature(keys)
            .finish()
            .blob()
        val bquery = queryBlob.toByteArray()

        var protoQuery: Query? = null
        try {
            protoQuery = Query.parseFrom(bquery)
        } catch (e: InvalidProtocolBufferException) {
            logger.error { "Exception while converting byte array to protobuf:" + e.message }
        }
        return protoQuery
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
            logger.error { "Exception while converting byte array to protobuf:" + e.message }
        }
        return protoQuery
    }


    /**
     * Prepares transaction before sending it to a peer
     * @param utx - unsigned model transaction
     * @param keys used to sign transaction
     * @return proto transaction, if succeeded
     */
    fun prepareTransaction(utx: UnsignedTx, keys: Keypair): Transaction? {
        // sign transaction and get its binary representation (Blob)
        val txblob = ModelProtoTransaction(utx)
            .signAndAddSignature(keys)
            .finish()
            .blob()

        // Convert ByteVector to byte array
        val bs = txblob.toByteArray()

        // create proto object
        val protoTx: Transaction?
        try {
            protoTx = Transaction.parseFrom(bs)
        } catch (e: InvalidProtocolBufferException) {
            logger.error { "Exception while converting byte array to protobuf:" + e.message }
            return null
        }

        return protoTx
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
     * Used to parse account detail
     * @param str raw json
     * @return map of string -> map of string -> string
     */
    fun jsonToKV(str: String): Map<String, Map<String, String>>? {

        val result = mutableMapOf<String, Map<String, String>>()
        val reader = JsonReader.of(Okio.buffer(Okio.source(ByteArrayInputStream(str.toByteArray()))))
        reader.beginObject()

        while (reader.hasNext()) {

            val key = reader.nextName()
            val curr = mutableMapOf<String, String>()
            reader.selectName(JsonReader.Options.of(key))
            reader.beginObject()
            while (reader.hasNext())
                curr[reader.nextName()] = reader.readJsonValue() as String

            result[key] = curr
            reader.endObject()
        }
        return result
    }


}
