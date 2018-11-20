package wdrollbackservice

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import model.IrohaCredential
import mu.KLogging
import provider.NotaryPeerListProviderImpl
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetwork
import withdrawalservice.WithdrawalTxDAOImpl
import java.util.*
import kotlin.concurrent.timerTask

/**
 * @param withdrawalConfig - configuration for withdrawal service
 */
class WdRollbackServiceInitialization(
    private val wdRollbackServiceConfig: WdRollbackServiceConfig,
    private val irohaNetwork: IrohaNetwork,
    private val credential: IrohaCredential
) {

    /**
     * Init Withdrawal Rollback Service
     */
    private fun initWithdrawalService(
        irohaNetwork: IrohaNetwork,
        credential: IrohaCredential,
        storageAccount: String,
        notaryListStorageAccount: String,
        notaryListSetterAccount: String
    ): WdRollbackService {
        logger.info { "Init Withdrawal Rollback Service" }

        return WdRollbackServiceImpl(
            WithdrawalTxDAOImpl(
                IrohaConsumerImpl(credential, irohaNetwork),
                credential,
                irohaNetwork,
                storageAccount
            ),
            NotaryPeerListProviderImpl(
                credential,
                irohaNetwork,
                notaryListStorageAccount,
                notaryListSetterAccount
            )
        )
    }

    fun init(): Result<Unit, Exception> {
        logger.info { "Start withdrawal rollback service init at iroha ${wdRollbackServiceConfig.iroha.hostname} : ${wdRollbackServiceConfig.iroha.port}" }

        return Result.of {
            val wdRollbackService = initWithdrawalService(
                irohaNetwork,
                credential,
                wdRollbackServiceConfig.withdrawalTxStorageAccount,
                wdRollbackServiceConfig.notaryListStorageAccount,
                wdRollbackServiceConfig.notaryListSetterAccount
            )
            Timer().schedule(timerTask {
                wdRollbackService.monitorWithdrawal()
                    .subscribe(
                        { res ->
                            res.map { rollbackEventDescriptions ->
                                rollbackEventDescriptions.map { description ->
                                    wdRollbackService.initiateRollback(description)
                                }
                            }
                                .failure { ex ->
                                    logger.error("Error during Iroha transaction", ex)
                                    throw ex
                                }
                        }, { ex ->
                            logger.error("Withdrawal events observable error", ex)
                        }
                    )
            }, 10000, 3000)
            Unit
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
