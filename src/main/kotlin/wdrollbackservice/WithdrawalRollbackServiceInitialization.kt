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

class WithdrawalRollbackServiceInitialization(
    private val withdrawalRollbackServiceConfig: WithdrawalRollbackServiceConfig,
    private val irohaNetwork: IrohaNetwork,
    private val credential: IrohaCredential
) {

    /**
     * Init Withdrawal Rollback Service
     */
    private fun initWithdrawalRollbackService(
        irohaNetwork: IrohaNetwork,
        credential: IrohaCredential,
        storageAccount: String,
        notaryListStorageAccount: String,
        notaryListSetterAccount: String
    ): WithdrawalRollbackService {
        logger.info { "Init Withdrawal Rollback Service" }

        return WithdrawalRollbackServiceImpl(
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
        logger.info { "Start withdrawal rollback service init at iroha ${withdrawalRollbackServiceConfig.iroha.hostname} : ${withdrawalRollbackServiceConfig.iroha.port}" }

        return Result.of {
            val withdrawalRollbackService = initWithdrawalRollbackService(
                irohaNetwork,
                credential,
                withdrawalRollbackServiceConfig.withdrawalTxStorageAccount,
                withdrawalRollbackServiceConfig.notaryListStorageAccount,
                withdrawalRollbackServiceConfig.notaryListSetterAccount
            )
            Timer().schedule(timerTask {
                withdrawalRollbackService.monitorWithdrawal()
                    .subscribe(
                        { res ->
                            res
                                .map { rollbackEventDescription ->
                                    withdrawalRollbackService.initiateRollback(rollbackEventDescription)

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
