package endpoint.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import io.grpc.ManagedChannelBuilder
import iroha.protocol.Queries
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.Hash
import jp.co.soramitsu.iroha.HashVector
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelQueryBuilder
import jp.co.soramitsu.iroha.ModelProtoQuery
import main.CONFIG
import main.ConfigKeys
import mu.KLogging
import sideChain.iroha.util.toByteArray
import java.math.BigInteger


class NotaryException(val reason: String) : Exception(reason)

/**
 * Class performs effective implementation of refund strategy for Ethereum
 */
class EthRefundStrategyImpl(private val keypair: Keypair) : EthRefundStrategy {

    override fun performRefund(request: EthRefundRequest): EthNotaryResponse {
        return performQuery(request)
            .flatMap { checkTransaction(it) }
            .flatMap { makeRefund(it) }
            .fold({ it },
                { EthNotaryResponse.Error(it.toString()) })
    }

    /**
     * The method passes the query with transaction to Iroha and return the answer
     * @param request - user's request with transaction hash
     * @return Transaction which is appeared in Iroha or error
     */
    private fun performQuery(request: EthRefundRequest): Result<iroha.protocol.BlockOuterClass.Transaction, Exception> {
        return Result.of {
            val hashes = HashVector()
            val hash = Hash(Hash.fromHexString(request.irohaTx))
            hashes.add(hash)

            val uquery = ModelQueryBuilder().creatorAccountId(CONFIG[ConfigKeys.irohaCreator])
                .queryCounter(BigInteger.valueOf(1))
                .createdTime(BigInteger.valueOf(System.currentTimeMillis()))
                .getTransactions(hashes)
                .build()
            val queryBlob = ModelProtoQuery(uquery).signAndAddSignature(keypair).finish().blob().toByteArray()
            val protoQuery = Queries.Query.parseFrom(queryBlob)

            val irohaHost = CONFIG[ConfigKeys.irohaHostname]
            val irohaPort = CONFIG[ConfigKeys.irohaPort]
            val channel = ManagedChannelBuilder.forAddress(irohaHost, irohaPort).usePlaintext(true).build()
            val queryStub = QueryServiceGrpc.newBlockingStub(channel)
            val queryResponse = queryStub.find(protoQuery)

            val fieldDescriptor =
                queryResponse.descriptorForType.findFieldByName("transactions_response")
            if (!queryResponse.hasField(fieldDescriptor)) {
                throw NotaryException("Query response error ${queryResponse.errorResponse}")
            }

            // return transaction
            queryResponse.transactionsResponse.transactionsList[0]
        }
    }

    /**
     * The method checks transaction and create refund if it is correct
     * @param appearedTx - target transaction from Iroha
     * @return Refund or error
     */
    private fun checkTransaction(appearedTx: iroha.protocol.BlockOuterClass.Transaction): Result<EthRefund, Exception> {
        return Result.of {
            val commands = appearedTx.payload.getCommands(0)
            when {
            // rollback case
                commands.hasSetAccountDetail() -> {
                    logger.info { "Has command SetAccountDetail" }
                    // TODO replace with effective implementation
                    EthRefund("mockAddress", "mockCoinType", BigInteger.TEN)
                }
            // Iroha -> Eth case
                commands.hasTransferAsset() -> {
                    logger.info { "Has command TransferAsset" }
                    // TODO replace with effective implementation
                    EthRefund("mockAddress", "mockCoinType", BigInteger.TEN)
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
        logger.info { "Make refund. Address: ${ethRefund.address}, amount: ${ethRefund.amount} ${ethRefund.type}" }
        return Result.of {
            // TODO replace with effective implementation
            EthNotaryResponse.Successful("mockSignature", ethRefund)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
