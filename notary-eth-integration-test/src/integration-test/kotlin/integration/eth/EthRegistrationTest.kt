package integration.eth

import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import integration.helper.EthIntegrationHelperUtil
import integration.registration.RegistrationServiceTestEnvironment
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import kotlinx.coroutines.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EthRegistrationTest {

    /** Integration tests util */
    private val integrationHelper = EthIntegrationHelperUtil()

    private val whitelist = "0x0000000000000000000000000000000000000000"

    private val registrationServiceEnvironment = RegistrationServiceTestEnvironment(integrationHelper)

    private val ethRegistrationConfig = integrationHelper.ethRegistrationConfig

    private val ethRegistrationService: Job

    init {
        registrationServiceEnvironment.registrationInitialization.init()

        ethRegistrationService = GlobalScope.launch {
            integrationHelper.runEthRegistrationService(ethRegistrationConfig)
        }
        runBlocking { delay(5_000) }
    }

    @AfterAll
    fun dropDown() {
        registrationServiceEnvironment.close()
        ethRegistrationService.cancel()
    }

    /**
     * Send POST request to local server
     */
    fun post(params: Map<String, String>): khttp.responses.Response {
        return khttp.post("http://127.0.0.1:${ethRegistrationConfig.port}/users", data = params)
    }

    /**
     * Test healthcheck
     * @given Registration service is up and running
     * @when GET query sent to healthcheck port
     * @then {"status":"UP"} is returned
     */
    @Test
    fun healthcheck() {
        val res = khttp.get("http://127.0.0.1:${ethRegistrationConfig.port}/actuator/health")
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
        // deploy free relay
        integrationHelper.deployRelays(1)
        val freeRelay = integrationHelper.getFreeRelay()

        val name = String.getRandomString(7)
        val pubkey = Ed25519Sha3().generateKeypair().public.toHexString()
        val clientId = "$name@d3"

        // register in Iroha
        var res = khttp.post(
            "http://127.0.0.1:${registrationServiceEnvironment.registrationConfig.port}/users", data = mapOf(
                "name" to name,
                "pubkey" to pubkey,
                "whitelist" to whitelist
            )
        )
        assertEquals(200, res.statusCode)
        // register in Eth
        res = khttp.post(
            "http://127.0.0.1:${ethRegistrationConfig.port}/users", data = mapOf(
                "name" to name,
                "pubkey" to pubkey,
                "whitelist" to whitelist
            )
        )
        assertEquals(200, res.statusCode)

        // check relay address
        assertEquals(freeRelay, integrationHelper.getRelaysByAccount(clientId).first())
        // check whitelist
        assert(integrationHelper.isWhitelisted(clientId, whitelist))
    }

    /**
     * Test registration
     * @given Registration service is up and running
     * @when POST query is sent to register a user with `name` and `pubkey` where user with 'name' already exists
     * @then error response that userId already exists returned
     */
    @Test
    fun doubleRegistration() {
        // deploy free relay
        integrationHelper.deployRelays(1)
        val freeRelay = integrationHelper.getFreeRelay()

        val name = String.getRandomString(7)
        val pubkey = Ed25519Sha3().generateKeypair().public.toHexString()
        val clientId = "$name@d3"

        // register client in Iroha
        var res = khttp.post(
            "http://127.0.0.1:${registrationServiceEnvironment.registrationConfig.port}/users", data = mapOf(
                "name" to name,
                "pubkey" to pubkey,
                "whitelist" to whitelist
            )
        )
        assertEquals(200, res.statusCode)
        // register in eth
        res = khttp.post(
            "http://127.0.0.1:${ethRegistrationConfig.port}/users", data = mapOf(
                "name" to name,
                "pubkey" to pubkey,
                "whitelist" to whitelist
            )
        )
        assertEquals(200, res.statusCode)

        // check relay address
        assertEquals(freeRelay, integrationHelper.getRelaysByAccount(clientId).first())
        // check whitelist
        assert(integrationHelper.isWhitelisted(clientId, whitelist))


        // deploy free relay
        integrationHelper.deployRelays(1)
        val anotherWhitelist = "0x0000000000000000000000000000000000000123"
        // try to register with the same name
        res = post(
            mapOf(
                "name" to name,
                "pubkey" to pubkey,
                "whitelist" to anotherWhitelist
            )
        )
        assertEquals(500, res.statusCode)

        // check relay address the same
        assertEquals(freeRelay, integrationHelper.getRelaysByAccount(clientId).first())
        // check whitelist the same
        assert(integrationHelper.isWhitelisted(clientId, whitelist))
        assert(!integrationHelper.isWhitelisted(clientId, anotherWhitelist))
    }

}
