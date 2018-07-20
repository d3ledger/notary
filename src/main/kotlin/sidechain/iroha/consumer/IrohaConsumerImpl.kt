package sidechain.iroha.consumer

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import config.IrohaConfig
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.UnsignedTx
import mu.KLogging
import sidechain.iroha.util.ModelUtil

/**
 * Endpoint of Iroha to write transactions
 * @param irohaConfig Iroha configurations
 */
class IrohaConsumerImpl(irohaConfig: IrohaConfig) : IrohaConsumer {

    val keypair = ModelUtil.loadKeypair(irohaConfig.pubkeyPath, irohaConfig.privkeyPath).get()

    val irohaNetwork = IrohaNetworkImpl(irohaConfig.hostname, irohaConfig.port)

    /**
     * Send transaction to Iroha and check if it is committed with status stream
     * @param utx - unsigned transaction to send
     * @return Result with string representation of hash or possible failure
     */
    override fun sendAndCheck(utx: UnsignedTx): Result<String, Exception> {
        val hash = utx.hash()
        return ModelUtil.prepareTransaction(utx, keypair)
            .flatMap { irohaNetwork.sendAndCheck(it, hash) }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
