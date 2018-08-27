package provider

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.IrohaConfig
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil

class TriggerProvider(
    irohaConfig: IrohaConfig,
    private val triggeredAccount: String,
    private val triggerCallerAccount: String
) {
    private val irohaConsumer = IrohaConsumerImpl(irohaConfig)

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
