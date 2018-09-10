package provider

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.IrohaConfig
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil

/*
    Provider that helps us to implement pub/sub mechanism in Iroha using account as an event source.
 */
class TriggerProvider(
    irohaConfig: IrohaConfig,
    private val triggeredAccount: String,
    private val triggerCallerAccount: String
) {
    private val irohaConsumer = IrohaConsumerImpl(irohaConfig)

    /**
     * Triggers triggeredAccount by setting details
     *
     * @param payload - some data to store
     * @return Result of detail setting process
     */
    fun trigger(payload: String): Result<Unit, Exception> {
        return ModelUtil.setAccountDetail(
            irohaConsumer,
            triggerCallerAccount,
            triggeredAccount,
            payload,
            ""
        ).map {
            Unit
        }
    }
}
