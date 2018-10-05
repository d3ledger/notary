package sidechain.iroha.consumer

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import config.IrohaConfig
import jp.co.soramitsu.iroha.UnsignedTx
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.util.ModelUtil

/**
 * Endpoint of Iroha to write transactions
 * @param irohaCredential for creating transactions
 * @param irohaConfig Iroha configurations
 */
class IrohaConsumerImpl(irohaCredential: IrohaCredential, irohaConfig: IrohaConfig) : IrohaConsumer {

    override val creator =  irohaCredential.accountId

    val keypair = irohaCredential.keyPair

    val irohaNetwork = IrohaNetworkImpl(irohaConfig.hostname, irohaConfig.port)

    /**
     * Send transaction to Iroha and check if it is committed with status stream
     * @param utx - unsigned transaction to send
     * @return Result with string representation of hash or possible failure
     */
    override fun sendAndCheck(utx: UnsignedTx): Result<String, Exception> {
        val hash = utx.hash()
        return ModelUtil.prepareTransaction(utx, keypair)
            .flatMap {
                irohaNetwork.sendAndCheck(it, hash)
            }
    }

    /**
     * Send a batch of transactions to Iroha and check if it is committed with status stream
     * @param lst - list of unsigned transaction to send
     * @return Result with list of passed transactions
     */
    override fun sendAndCheck(lst: List<UnsignedTx>): Result<List<String>, Exception> {
        val batch = lst.map { tx ->
            ModelUtil.prepareTransaction(tx, keypair).get()
        }
        val hashes = lst.map { it.hash() }
        return irohaNetwork.sendAndCheck(batch, hashes)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
