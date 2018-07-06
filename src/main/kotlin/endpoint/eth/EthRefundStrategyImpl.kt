package endpoint.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import io.grpc.ManagedChannelBuilder
import iroha.protocol.BlockOuterClass.Transaction
import iroha.protocol.Queries
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.*
import main.ConfigKeys
import mu.KLogging
import notary.CONFIG
import sidechain.iroha.util.toBigInteger
import sidechain.iroha.util.toByteArray
import java.math.BigInteger

class NotaryException(val reason: String) : Exception(reason)

/**
 * Class performs effective implementation of refund strategy for Ethereum
 */
class EthRefundStrategyImpl(private val keypair: Keypair) : EthRefundStrategy {

    override fun performRefund(request: EthRefundRequest): EthNotaryResponse {
        return performQuery(request)
            .flatMap { checkTransaction(it, request) }
            .flatMap { makeRefund(it) }
            .fold({ it },
                { EthNotaryResponse.Error(it.toString()) })
    }

    /**
     * The method passes the query with transaction to Iroha and return the answer
     * @param request - user's request with transaction hash
     * @return Transaction which is appeared in Iroha or error
     */
    private fun performQuery(request: EthRefundRequest): Result<Transaction, Exception> {
        return Result.of {

            val hashes = HashVector()
            hashes.add(Hash.fromHexString(request.irohaTx))

            val uquery = ModelQueryBuilder().creatorAccountId(CONFIG[ConfigKeys.notaryIrohaAccount])
                .queryCounter(BigInteger.valueOf(1))
                .createdTime(BigInteger.valueOf(System.currentTimeMillis()))
                .getTransactions(hashes)
                .build()
            val queryBlob = ModelProtoQuery(uquery).signAndAddSignature(keypair).finish().blob().toByteArray()
            val protoQuery = Queries.Query.parseFrom(queryBlob)

            val irohaHost = CONFIG[ConfigKeys.notaryIrohaHostname]
            val irohaPort = CONFIG[ConfigKeys.notaryIrohaPort]
            val channel = ManagedChannelBuilder.forAddress(irohaHost, irohaPort).usePlaintext(true).build()
            val queryStub = QueryServiceGrpc.newBlockingStub(channel)
            val queryResponse = queryStub.find(protoQuery)

            val fieldDescriptor =
                queryResponse.descriptorForType.findFieldByName("transactions_response")
            if (!queryResponse.hasField(fieldDescriptor)) {
                throw NotaryException("Query response error ${queryResponse.errorResponse}")
            }

            if (queryResponse.transactionsResponse.transactionsCount == 0)
                throw Exception("Withdrawal service. Transaction ${request.irohaTx} not found.")

            // return transaction
            queryResponse.transactionsResponse.transactionsList[0]
        }
    }

    /**
     * The method checks transaction and create refund if it is correct
     * @param appearedTx - target transaction from Iroha
     * @param request - user's request with transaction hash
     * @return Refund or error
     */
    private fun checkTransaction(
        appearedTx: Transaction,
        request: EthRefundRequest
    ): Result<EthRefund, Exception> {
        return Result.of {
            val commands = appearedTx.payload.reducedPayload.getCommands(0)

            when {
            // rollback case
                appearedTx.payload.reducedPayload.commandsCount == 1 &&
                        commands.hasSetAccountDetail() -> {

                    // TODO a.chernyshov replace with effective implementation
                    // 1. Get eth transaction hash from setAccountDetail
                    // 2. Check eth transaction and get info from it
                    //    There should be a batch with 3 txs
                    //      if 3 tx in the batch - reject rollback
                    //      if 2 tx (transfer asset is absent) - approve rollback
                    // 3. build EthRefund

                    val key = commands.setAccountDetail.key
                    val value = commands.setAccountDetail.value
                    val destEthAddress = ""
                    logger.info { "Rollback case ($key, $value)" }

                    EthRefund(destEthAddress, "mockCoinType", BigInteger.TEN, request.irohaTx)
                }
            // withdrawal case
                (appearedTx.payload.reducedPayload.commandsCount == 1) &&
                        commands.hasTransferAsset() -> {
                    val destAccount = commands.transferAsset.destAccountId
                    if (destAccount != CONFIG[ConfigKeys.notaryIrohaAccount])
                        throw NotaryException("Refund - check transaction. Destination account is wrong '$destAccount'")

                    val amount = commands.transferAsset.amount.value.toBigInteger()
                    val token = commands.transferAsset.assetId.dropLastWhile { it != '#' }.dropLast(1)
                    val destEthAddress = commands.transferAsset.description

                    EthRefund(destEthAddress, token, amount, request.irohaTx)
                }
                else -> {
                    logger.error { "Transaction doesn't contain expected commands." }
                    throw NotaryException("Transaction doesn't contain expected commands.")
                }
            }
        }
    }

    /**
     * The method signs refund and return valid parameters for Ethereum smart contract call
     * @param ethRefund - refund for signing
     * @return signed refund or error
     */
    private fun makeRefund(ethRefund: EthRefund): Result<EthNotaryResponse, Exception> {
        logger.info { "Make refund. Address: ${ethRefund.address}, amount: ${ethRefund.amount} ${ethRefund.assetId}" }
        return Result.of {
            // TODO a.chernyshov replace with effective implementation
            EthNotaryResponse.Successful("mockSignature", ethRefund)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
