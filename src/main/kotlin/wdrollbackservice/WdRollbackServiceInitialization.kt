package wdrollbackservice

import com.github.kittinunf.result.Result
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetwork
import withdrawalservice.WithdrawalServiceConfig
import withdrawalservice.WithdrawalTxDAOImpl

/**
 * @param withdrawalConfig - configuration for withdrawal service
 */
class WdRollbackServiceInitialization(
    private val withdrawalServiceConfig: WithdrawalServiceConfig,
    private val irohaNetwork: IrohaNetwork,
    private val credential: IrohaCredential
) {

    /**
     * Init Withdrawal Rollback Service
     */
    private fun initWithdrawalService(
        irohaNetwork: IrohaNetwork,
        credential: IrohaCredential,
        notaryAccount: String
    ): Result<WdRollbackService, Exception> {
        logger.info { "Init Withdrawal Rollback Service" }

        return Result.of {
            WdRollbackServiceImpl(
                irohaNetwork,
                credential,
                WithdrawalTxDAOImpl(
                    IrohaConsumerImpl(credential, irohaNetwork),
                    credential,
                    irohaNetwork,
                    notaryAccount
                ).getObservable(),
                notaryAccount
            )
        }
    }

    fun init(): Result<WdRollbackService, Exception> {
        logger.info { "Start withdrawal rollback service init at iroha ${withdrawalServiceConfig.iroha.hostname} : ${withdrawalServiceConfig.iroha.port}" }
        return initWithdrawalService(
            irohaNetwork,
            credential,
            withdrawalServiceConfig.notaryIrohaAccount
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
