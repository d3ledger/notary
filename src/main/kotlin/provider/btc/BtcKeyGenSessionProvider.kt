package provider.btc

import com.github.kittinunf.result.Result
import config.IrohaConfig
import jp.co.soramitsu.iroha.Keypair
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil

class BtcKeyGenSessionProvider(
    private val irohaConfig: IrohaConfig,
    private val keypair: Keypair
) {
    private val irohaConsumer = IrohaConsumerImpl(irohaConfig)
    fun createSession(sessionId: String): Result<String, Exception> {
        return ModelUtil.createAccount(irohaConsumer, keypair, irohaConfig.creator, sessionId)
    }
}
