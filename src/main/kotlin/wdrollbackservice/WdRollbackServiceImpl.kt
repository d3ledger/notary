package wdrollbackservice

import com.github.kittinunf.result.Result
import io.reactivex.Observable
import mu.KLogging
import provider.NotaryPeerListProvider
import withdrawalservice.WithdrawalTxDAO
import java.util.stream.Collectors

class WdRollbackServiceImpl(
    private val withdrawalTransactionsDAO: WithdrawalTxDAO<String, String>,
    private val notaryPeerListProvider: NotaryPeerListProvider
) : WdRollbackService {

    /**
     * Handle withdrawal event to check its success
     * @param withdrawalIrohaEventHash - iroha event hash
     * @param ethTxStatus - eth transaction status
     * @return event description based on the Ethereum transaction result
     */
    private fun processIrohaEvent(
        withdrawalIrohaEventHash: String,
        ethTxStatus: String?
    ): Result<List<WdRollbackServiceOutputEvent>, Exception> {
        return Result.of {
            listOf(
                WdRollbackServiceOutputEvent(
                    withdrawalIrohaEventHash,
                    ethFailedStatuses.contains(ethTxStatus)
                )
            )
        }
    }

    /**
     * Create and send transaction for assets refunding in Iroha in case of rollback
     * @param wdRollbackServiceOutputEvent - Ethereum event outcome for a given Iroha withdrawal transaction
     * @return hex representation of Iroha transaction hash or an empty string if rollback was not required
     */
    override fun initiateRollback(wdRollbackServiceOutputEvent: WdRollbackServiceOutputEvent): Result<String, Exception> {
        if (!wdRollbackServiceOutputEvent.isRollbackRequired) {
            return Result.of("")
        }
        val irohaTxHash = wdRollbackServiceOutputEvent.irohaTxHash
        return Result.of {
            notaryPeerListProvider.getPeerList().stream().map {
                val res = khttp.get("$it/ethwdrb/$irohaTxHash")
                if (res.statusCode == 200) {
                    logger.info("Received correct response status of withdrawal rollback for $irohaTxHash")
                    withdrawalTransactionsDAO.remove(irohaTxHash)
                } else {
                    logger.error("Received invalid response status code for $irohaTxHash")
                    throw Exception("Invalid response status code")
                }
                // TODO: Rewrite when implementing quorum
            }.collect(Collectors.toList())[0].toString()
        }
    }


    override fun monitorWithdrawal(): Observable<Result<List<WdRollbackServiceOutputEvent>, Exception>> {
        // TODO: provide synchronization if rollback service is not singleton
        return withdrawalTransactionsDAO.getObservable()
            .map {
                processIrohaEvent(it.key, it.value)
            }
    }

    /**
     * Logger
     */
    companion object : KLogging() {
        private val ethFailedStatuses = setOf("0x0", "null")
    }
}
