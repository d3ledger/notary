package sidechain.iroha.consumer

import com.github.kittinunf.result.Result
import jp.co.soramitsu.iroha.UnsignedTx

/**
 * Interface for consuming Iroha events provided by [notary.Notary]
 */
interface IrohaConsumer {

    /**
     * Send transaction to Iroha and check if it is committed with status stream
     * @param utx - unsigned transaction to send
     * @return Result with string representation of hash or possible failure
     */
    fun sendAndCheck(utx: UnsignedTx): Result<String, Exception>

    fun sendAndCheck(lst: List<UnsignedTx>): Result<List<String>, Exception>
}
