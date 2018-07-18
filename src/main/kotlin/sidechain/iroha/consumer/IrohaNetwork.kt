package sidechain.iroha.consumer

import com.github.kittinunf.result.Result
import iroha.protocol.BlockOuterClass
import iroha.protocol.Responses
import jp.co.soramitsu.iroha.Hash

/**
 * Interface for network layer of Iroha chain
 */
interface IrohaNetwork {

    /**
     * Send transaction to Iroha and check if it is committed with status stream
     * @param tx - transaction to send
     * @param hash - hash of transaction
     * @return Result with string representation of hash or possible failure
     */
    fun sendAndCheck(tx: BlockOuterClass.Transaction, hash: Hash): Result<String, Exception>

    /**
     * Send query to Iroha
     * @param protoQuery - protobuf representation of query
     */
    fun sendQuery(protoQuery: iroha.protocol.Queries.Query): Result<Responses.QueryResponse, Exception>
}
