package wdrollbackservice

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import model.IrohaCredential
import mu.KLogging
import sidechain.SideChainEvent
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.ModelUtil
import withdrawalservice.WithdrawalTxDAO

class WdRollbackServiceImpl(
    private val irohaNetwork: IrohaNetwork,
    private val credential: IrohaCredential,
    private val withdrawalTransactionsDAO: WithdrawalTxDAO<String, String?>,
    private val notaryAccount: String
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
    ): Result<WdRollbackServiceOutputEvent, Exception> {
        return Result.of {
            WdRollbackServiceOutputEvent(
                withdrawalIrohaEventHash,
                ethTxStatus == null || ethTxStatus == ETH_STATUS_FAIL
            )
        }
    }

    /**
     * Create and send transaction for assets refunding in Iroha in case of rollback
     * @param wdRollbackServiceOutputEvent - Ethereum event outcome for a given Iroha withdrawal transaction
     * @return hex representation of Iroha transaction hash or an empty string if rollback was not required
     */
    private fun initiateRollback(wdRollbackServiceOutputEvent: WdRollbackServiceOutputEvent): Result<String, Exception> {
        if (!wdRollbackServiceOutputEvent.isRollbackRequired) {
            return Result.of("")
        }

        var rollbackIrohaTxHash: Result<String, Exception>

        return ModelUtil.getTransaction(
            irohaNetwork,
            credential,
            wdRollbackServiceOutputEvent.irohaTxHash
        )
            .map { tx ->
                tx.payload.reducedPayload.commandsList.first { command ->
                    val transferAsset = command.transferAsset
                    transferAsset?.srcAccountId != "" && transferAsset?.destAccountId == notaryAccount
                }
            }
            .flatMap { transferCommand ->
                val destAccountId = transferCommand?.transferAsset?.srcAccountId
                    ?: throw IllegalStateException("Unable to identify primary Iroha transaction data")

                rollbackIrohaTxHash = ModelUtil.transferAssetIroha(
                    IrohaConsumerImpl(credential, irohaNetwork),
                    notaryAccount,
                    destAccountId,
                    transferCommand.transferAsset.assetId,
                    "Rollback transaction in Iroha for $destAccountId due to error during withdrawal in Ethereum",
                    transferCommand.transferAsset.amount
                )
                withdrawalTransactionsDAO.remove(wdRollbackServiceOutputEvent.irohaTxHash)
                rollbackIrohaTxHash
            }
    }

    override fun monitorWithdrawal(): Result<Unit, Exception> {
        // TODO: provide synchronization if rollback service is not singleton
        return Result.of {
            withdrawalTransactionsDAO.getObservable().subscribe({ res ->
                processIrohaEvent(res.key, res.value)
                    .map { outcome ->
                        initiateRollback(outcome)
                    }
                    .failure { ex ->
                        logger.error("Error during Iroha rollback initiation for ${res.key}", ex)
                        throw ex
                    }
            }, { ex ->
                logger.error("Withdrawal observable error", ex)
            }
            )
            Unit
        }
    }

    /**
     * Logger
     */
    companion object : KLogging() {
        const val ETH_STATUS_FAIL = "0x0"
    }
}
