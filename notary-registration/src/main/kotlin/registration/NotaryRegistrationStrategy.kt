package registration

import com.github.kittinunf.result.Result
import jp.co.soramitsu.iroha.PublicKey
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import sidechain.iroha.CLIENT_DOMAIN
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.util.ModelUtil

/**
 * Strategy to register client account in D3. This strategy creates only Iroha account.
 */
@Component
class NotaryRegistrationStrategy(
    @Autowired private val irohaConsumer: IrohaConsumer
) : RegistrationStrategy {

    /**
     * Register a new D3 client in Iroha
     * @param name - unique user name
     * @param pubkey - client public key
     * @return hash of tx in Iroha
     */
    override fun register(name: String, whitelist: List<String>, pubkey: String): Result<String, Exception> {
        logger.info { "notary registration of client $name with pubkey $pubkey" }
        return ModelUtil.createAccount(
            irohaConsumer,
            name,
            CLIENT_DOMAIN,
            PublicKey(PublicKey.fromHexString(pubkey))
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
