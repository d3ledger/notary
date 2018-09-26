package provider

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.IrohaConfig
import mu.KLogging
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil

/*
    Provider that helps us to implement pub/sub mechanism in Iroha using account as an event source.
 */
class TriggerProvider(
    irohaConfig: IrohaConfig,
    private val triggeredAccount: String
) {
    init {
        logger.info {
            "Init trigger provider with triggered account '$triggeredAccount' and trigger caller account '${irohaConsumer.creator}'"
        }
    }

    private val irohaConsumer = IrohaConsumerImpl(irohaConfig.creator, irohaConfig)

    /**
     * Triggers triggeredAccount by setting details
     *
     * @param payload - some data to store
     * @return Result of detail setting process
     */
    fun trigger(payload: String): Result<Unit, Exception> {
        return ModelUtil.setAccountDetail(
            irohaConsumer,
            triggeredAccount,
            payload,
            ""
        ).map {
            logger.info { "${irohaConsumer.creator} was triggered with payload $payload" }
            Unit
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
