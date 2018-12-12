import com.github.kittinunf.result.failure
import config.loadConfigs
import jp.co.soramitsu.iroha.ModelCrypto
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import registration.NotaryRegistrationConfig
import registration.main
import sidechain.iroha.IrohaInitialization
import util.getRandomString
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotaryRegistrationTest {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure { fail(it) }
    }

    val registrationConfig =
        loadConfigs("registration", NotaryRegistrationConfig::class.java, "/registration.properties").get()

    init {
        GlobalScope.launch {
            main(emptyArray())
        }

        runBlocking { delay(20_000) }
    }

    /**
     * Send POST request to local server
     */
    fun post(params: Map<String, String>): khttp.responses.Response {
        return khttp.post("http://127.0.0.1:${registrationConfig.port}/users", data = params)
    }

    /**
     * Test healthcheck
     * @given Registration service is up and running
     * @when GET query sent to healthcheck port
     * @then {"status":"UP"} is returned
     */
    @Test
    fun healthcheck() {
        val res = khttp.get("http://127.0.0.1:${registrationConfig.healthCheckPort}/health")
        assertEquals(200, res.statusCode)
        assertEquals("{\"status\":\"UP\"}", res.text)
    }

    /**
     * Test registration
     * @given Registration service is up and running
     * @when POST query is sent to register a user with `name` and `pubkey`
     * @then new account is created in Iroha
     */
    @Test
    fun correctRegistration() {
        val name = String.getRandomString(9)
        val pubkey = ModelCrypto().generateKeypair().publicKey().hex()

        val res = post(
            mapOf(
                "name" to name,
                "pubkey" to pubkey
            )
        )

        assertEquals(200, res.statusCode)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
