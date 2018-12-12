package sidechain.iroha.consumer

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import iroha.protocol.Endpoint
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import model.IrohaCredential
import mu.KLogging
import javax.xml.bind.DatatypeConverter

/**
 * Endpoint of Iroha to write transactions
 * @param irohaCredential for creating transactions
 * @param irohaAPI Iroha network
 */
class IrohaConsumerImpl(
    irohaCredential: IrohaCredential,
    private val irohaAPI: IrohaAPI
) : IrohaConsumer {
    override val creator = irohaCredential.accountId

    val keypair = irohaCredential.keyPair

    /**
     * Send transaction to Iroha and check if it is committed with status stream
     * @param utx - unsigned transaction to send
     * @return byte representation of hash or failure
     */
    override fun send(utx: Transaction): Result<ByteArray, Exception> {
        val transaction = utx.sign(keypair).build()
        return Result.of {
            irohaAPI.transactionSync(transaction)
        }.flatMap {
            checkTransactionStatus(Utils.hash(transaction))
        }
    }

    /**
     * Send list of transactions to Iroha
     * @param lst - list of unsigned transactions to send
     * @return byte representation of hash or failure
     */
    override fun send(lst: List<Transaction>): Result<List<ByteArray>, Exception> {
        val batch = lst.map { tx ->
            tx.sign(keypair).build()
        }
        logger.info { "Send TX batch to IROHA" }
        irohaAPI.transactionListSync(batch)
        // TODO: Fix later. Otherwise all transactions have status UNRECOGNIZED and filter is senseless
        Thread.sleep(1000)
        return Result.of {
            batch.map {
                Utils.hash(it)
            }
        }.map { hashes ->
            hashes.map {
                checkTransactionStatus(it)
            }
                .filter { res ->
                    res.fold(
                        { true },
                        {
                            logger.warn("Batch tx was failed: ", it)
                            false
                        }
                    )
                }.map { it.get() }
        }
    }

    /**
     * Check if transaction is committed to Iroha
     * @param hash - hash of transaction to check
     * @return byte representation of hash or failure
     */
    private fun checkTransactionStatus(hash: ByteArray): Result<ByteArray, Exception> {
        return Result.of {
            val response = irohaAPI.txStatusSync(hash)
            val status = response.txStatus
            logger.info(status.toString())
            if (status == Endpoint.TxStatus.STATEFUL_VALIDATION_FAILED) {
                val message =
                    "Iroha transaction ${DatatypeConverter.printHexBinary(hash)} received STATEFUL_VALIDATION_FAILED ${response.errorMessage}"
                logger.error { message }
                throw Exception(message)
            }
            hash
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
