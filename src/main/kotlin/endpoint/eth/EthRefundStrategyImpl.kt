package endpoint.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import jp.co.soramitsu.iroha.Hash
import jp.co.soramitsu.iroha.HashVector
import jp.co.soramitsu.iroha.ModelQueryBuilder
import sideChain.iroha.IrohaSigner


class NotaryException(val code: Int, val reason: String) : Exception()

/**
 * Class performs effective implementation of refund strategy for Ethereum
 */
class EthRefundStrategyImpl(private val irohaSigner: IrohaSigner) : EthRefundStrategy {

    override fun performRefund(request: EthRefundRequest): EthNotaryResponse {

        return performQuery(request)
            .flatMap { checkTransaction(it) }
            .flatMap { makeRefund(it) }
            .fold({ it },
                { EthNotaryResponse.Error(it.code, it.reason) })
    }

    /**
     * The method passes the query with transaction to Iroha and return the answer
     * @param request - user's request with transaction hash
     * @return Transaction which is appeared in Iroha or error
     */
    private fun performQuery(request: EthRefundRequest): Result<iroha.protocol.BlockOuterClass.Transaction, NotaryException> {
        val query = ModelQueryBuilder()
        val hashes = HashVector()
        hashes.add(Hash(request.irohaTx))
        val signedQuery = irohaSigner.signQuery(query.getTransactions(hashes).build())

        TODO("send query to iroha and return answer")

    }

    /**
     * The method checks transaction and create refund if it is correct
     * @param appearedTx - target transaction from Iroha
     * @return Refund or error
     */
    private fun checkTransaction(appearedTx: iroha.protocol.BlockOuterClass.Transaction): Result<EthRefund, NotaryException> {
        val commands = appearedTx.payload.getCommands(0)
        // rollback case
        if (commands.hasSetAccountDetail()) {
            TODO("rollback case")
        }

        // Iroha -> Eth case
        if (commands.hasTransferAsset()) {
            TODO("Withdraw")
        }

        TODO("on fake transaction")
    }

    /**
     * The method signs refund and return valid parameters for Ethereum smart contract call
     * @param ethRefund - refund for signing
     * @return signed refund or error
     */
    private fun makeRefund(ethRefund: EthRefund): Result<EthNotaryResponse, NotaryException> {
        TODO("implement: combine of data and signing with Eth encoding")
    }

}
