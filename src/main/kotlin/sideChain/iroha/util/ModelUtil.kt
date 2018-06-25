package sideChain.iroha.util

import com.google.protobuf.InvalidProtocolBufferException
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import iroha.protocol.Queries
import jp.co.soramitsu.iroha.*
import mu.KLogging
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import iroha.protocol.Queries.Query
import iroha.protocol.Responses.QueryResponse
import iroha.protocol.BlockOuterClass.Transaction
import iroha.protocol.CommandServiceGrpc
import iroha.protocol.QueryServiceGrpc
import java.math.BigInteger

val logger = KLogging().logger

fun getModelCrypto() = ModelCrypto()

fun getModelTransactionBuilder() = ModelTransactionBuilder()

fun getModelQueryBuilder() = ModelQueryBuilder()

fun getCurrentTime() = BigInteger.valueOf(System.currentTimeMillis())

fun getChannel(host: String = "localhost", ip: Int = 50051) =
    ManagedChannelBuilder.forAddress(host, ip).usePlaintext(true).build()

fun getCommandStub(
    channel: ManagedChannel = getChannel()
): CommandServiceGrpc.CommandServiceBlockingStub =
    CommandServiceGrpc.newBlockingStub(channel)

fun getQueryStub(
    channel: ManagedChannel = getChannel()
): QueryServiceGrpc.QueryServiceBlockingStub =
    QueryServiceGrpc.newBlockingStub(channel)

fun readKeyFromFile(path: String): String? {
    return try {
        String(Files.readAllBytes(Paths.get(path)))
    } catch (e: IOException) {
        logger.error { "Unable to read key files.\n $e" }
        null
    }
}

fun getKeys(path: String, user: String) = getModelCrypto().convertFromExisting(
    readKeyFromFile("$path/$user.pub"),
    readKeyFromFile("$path/$user.priv")
)

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

fun isStatelessValid(resp: QueryResponse) =
    !(resp.hasErrorResponse() &&
            resp.errorResponse.reason.toString() == "STATELESS_INVALID")


