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
     * Send transaction to Iroha
     * @param protoTx protobuf representation of transaction
     */
    fun send(protoTx: BlockOuterClass.Transaction)

    /**
     * Send transaction to Iroha and check if it is committed with status stream
     * @param protoTx - transaction to send
     * @param hash - hash of transaction
     * @return Result with possible failure
     */
    fun sendAndCheck(protoTx: BlockOuterClass.Transaction, hash: Hash): Result<Unit, Exception>

    /**
     * Send query to Iroha
     * @param protoQuery - protobuf representation of query
     */
    fun sendQuery(protoQuery: iroha.protocol.Queries.Query): Result<Responses.QueryResponse, Exception>
}
