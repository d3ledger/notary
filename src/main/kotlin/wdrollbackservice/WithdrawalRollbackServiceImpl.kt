package wdrollbackservice

import com.github.kittinunf.result.Result
import io.reactivex.Observable
import mu.KLogging
import provider.NotaryPeerListProvider
import withdrawalservice.WithdrawalTxDAO
import java.util.stream.Collectors

class WithdrawalRollbackServiceImpl(
    private val withdrawalTransactionsDAO: WithdrawalTxDAO<String, String>,
    private val notaryPeerListProvider: NotaryPeerListProvider
) : WithdrawalRollbackService {

    /**
     * Handle withdrawal event to check its success
     * @param withdrawalIrohaEventHash - iroha event hash
     * @param ethTxStatus - eth transaction status
     * @return event description based on the Ethereum transaction result
     */
    private fun processIrohaEvent(
        withdrawalIrohaEventHash: String,
        ethTxStatus: String?
    ): Result<WithdrawalRollbackServiceOutputEvent, Exception> {
        return Result.of {
            WithdrawalRollbackServiceOutputEvent(
                withdrawalIrohaEventHash,
                ethFailedStatuses.contains(ethTxStatus)
            )
        }
    }

    /**
     * Create and send transaction for assets refunding in Iroha in case of rollback
     * @param withdrawalRollbackServiceOutputEvent - Ethereum event outcome for a given Iroha withdrawal transaction
     * @return hex representation of Iroha transaction hash or an empty string if rollback was not required
     */
    override fun initiateRollback(withdrawalRollbackServiceOutputEvent: WithdrawalRollbackServiceOutputEvent): Result<String, Exception> {
        if (!withdrawalRollbackServiceOutputEvent.isRollbackRequired) {
            return Result.of("")
        }
        val irohaTxHash = withdrawalRollbackServiceOutputEvent.irohaTxHash
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


    override fun monitorWithdrawal(): Observable<Result<WithdrawalRollbackServiceOutputEvent, Exception>> {
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
