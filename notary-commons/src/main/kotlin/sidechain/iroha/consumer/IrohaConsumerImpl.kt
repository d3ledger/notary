package sidechain.iroha.consumer

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import iroha.protocol.Endpoint
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import model.IrohaCredential
import mu.KLogging
import util.hex

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
     * Send transaction to Iroha and check if it is committed
     * @param utx - unsigned transaction to send
     * @return hex representation of hash or failure
     */
    override fun send(utx: Transaction): Result<String, Exception> {
        val transaction = utx.sign(keypair).build()
        return send(transaction)
    }

    /**
     * Send transaction to Iroha and check if it is committed
     * @param tx - built protobuf iroha transaction
     * @return hex representation of hash or failure
     */
    override fun send(tx: TransactionOuterClass.Transaction): Result<String, Exception> {
        return Result.of {
            irohaAPI.transactionSync(tx)
        }.flatMap {
            checkTransactionStatus(Utils.hash(tx))
        }
    }

    /**
     * Send list of transactions to Iroha and check if it is committed
     * @param lst - list of unsigned transactions to send
     * @return map of relevant hashes and boolean indicating if the tx is accepted by iroha
     */
    override fun send(lst: List<Transaction>): Result<Map<String, Boolean>, Exception> {
        val batch = lst.map { tx -> tx.build() }
        val irohaBatch = Utils.createTxOrderedBatch(batch, keypair)
        val acceptedHashes = send(irohaBatch).get()
        return Result.of {
            irohaBatch.map { tx ->
                String.hex(Utils.hash(tx))
            }.map { hash ->
                hash to acceptedHashes.contains(hash)
            }.toMap()
        }
    }

    /**
     * Send list of transactions to Iroha as BATCH and check if it is committed
     * @param lst - list of built protobuf iroha transactions
     * @return list of hex hashes that were accepted by iroha
     */
    override fun send(lst: Iterable<TransactionOuterClass.Transaction>): Result<List<String>, Exception> {
        irohaAPI.transactionListSync(lst)
        return Result.of {
            lst.map {
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
     * @return hex representation of hash or failure
     */
    private fun checkTransactionStatus(hash: ByteArray): Result<String, Exception> {
        return Result.of {
            val responseObservable = irohaAPI.cmdStub.statusStream(Utils.createTxStatusRequest(hash))
            val hex = String.hex(hash)
            responseObservable.forEachRemaining { statusResponse ->
                if (statusResponse.txStatus == Endpoint.TxStatus.STATEFUL_VALIDATION_FAILED) {
                    val message =
                        "Iroha transaction $hex received STATEFUL_VALIDATION_FAILED ${statusResponse.errOrCmdName}"
                    logger.error { message }
                    throw Exception(message)
                }
            }
            hex
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
