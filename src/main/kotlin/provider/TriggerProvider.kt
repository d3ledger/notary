package provider

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.IrohaConfig
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil

/*
    Provider that helps us to implement pub/sub mechanism in Iroha using account as an event source.
 */
class TriggerProvider(
    irohaConfig: IrohaConfig,
    private val callerCredential: IrohaCredential,
    private val triggerAccount: String
) {
    init {
        logger.info {
            "Init trigger provider with triggered account '$triggerAccount' and trigger caller account '${callerCredential.accountId}'"
        }
    }

    private val irohaConsumer = IrohaConsumerImpl(callerCredential, irohaConfig)

    /**
     * Triggers triggeredAccount by setting details
     *
     * @param payload - some data to store
     * @return Result of detail setting process
     */
    fun trigger(payload: String): Result<Unit, Exception> {
        return ModelUtil.setAccountDetail(
            irohaConsumer,
            triggerAccount,
            payload,
            ""
        ).map {
            logger.info { "$triggerAccount was triggered with payload $payload" }
            Unit
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
