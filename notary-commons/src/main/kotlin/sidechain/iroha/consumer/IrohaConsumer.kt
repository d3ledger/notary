package sidechain.iroha.consumer

import com.github.kittinunf.result.Result
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.Transaction

/**
 * Interface for consuming Iroha events provided by [notary.Notary]
 */
interface IrohaConsumer {

    /** Iroha transactions creator */
    val creator: String

    /**
     * Send transaction to Iroha and check if it is committed
     * @param utx - unsigned transaction to send
     */
    fun send(utx: Transaction): Result<ByteArray, Exception>

    /**
     * Send transaction to Iroha and check if it is committed
     * @param tx - built protobuf iroha transaction
     */
    fun send(tx: TransactionOuterClass.Transaction): Result<ByteArray, Exception>

    /**
     * Send list of transactions to Iroha and check if it is committed
     * @param utx - unsigned transaction to send
     */
    fun send(lst: List<Transaction>): Result<List<ByteArray>, Exception>
}
