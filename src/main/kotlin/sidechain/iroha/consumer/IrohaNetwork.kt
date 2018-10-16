package sidechain.iroha.consumer

import com.github.kittinunf.result.Result
import io.reactivex.Observable
import iroha.protocol.QryResponses
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.Hash
import model.IrohaCredential
import java.io.Closeable

/**
 * Interface for network layer of Iroha chain
 */
interface IrohaNetwork : Closeable {

    /**
     * Send transaction to Iroha and check if it is committed with status stream
     * @param tx - transaction to send
     * @param hash - hash of transaction
     * @return Result with string representation of hash or possible failure
     */
    fun sendAndCheck(tx: TransactionOuterClass.Transaction, hash: Hash): Result<String, Exception>

    /**
     * Send batch os transactions to Iroha and check if it is committed with status stream
     * @param batch - list of transaction as a batch to send
     * @param hashes - hashes of transactions
     * @return Result with string representation of hashes that were committed or possible failure
     */
    fun sendAndCheck(
        batch: List<TransactionOuterClass.Transaction>,
        hashes: List<Hash>
    ): Result<List<String>, Exception>

    /**
     * Send query to Iroha
     * @param protoQuery - protobuf representation of query
     */
    fun sendQuery(protoQuery: iroha.protocol.Queries.Query): Result<QryResponses.QueryResponse, Exception>

    /**
     * Get block streaming.
     */
    fun getBlocksStreaming(credential: IrohaCredential): Result<Observable<QryResponses.BlockQueryResponse>, Exception>
}
