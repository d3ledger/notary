package wdrollbackservice

import com.github.kittinunf.result.Result
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetwork
import withdrawalservice.WithdrawalTxDAOImpl

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
        notaryAccount: String
    ): WdRollbackService {
        logger.info { "Init Withdrawal Rollback Service" }

        return WdRollbackServiceImpl(
            irohaNetwork,
            credential,
            WithdrawalTxDAOImpl(
                IrohaConsumerImpl(credential, irohaNetwork),
                credential,
                irohaNetwork,
                notaryAccount
            ),
            notaryAccount
        )
    }

    fun init(): Result<Unit, Exception> {
        logger.info { "Start withdrawal rollback service init at iroha ${wdRollbackServiceConfig.iroha.hostname} : ${wdRollbackServiceConfig.iroha.port}" }

        val wdRollbackService = initWithdrawalService(
            irohaNetwork,
            credential,
            wdRollbackServiceConfig.withdrawalTxStorageAccount
        )

        return wdRollbackService.monitorWithdrawal()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
