package integration.eth

import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomString
import com.d3.commons.util.irohaEscape
import com.d3.commons.util.toHexString
import com.google.gson.GsonBuilder
import integration.helper.EthIntegrationHelperUtil
import integration.registration.RegistrationServiceTestEnvironment
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
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

//    private val ethRegistrationService: Job
//
//    init {
//        registrationServiceEnvironment.registrationInitialization.init()
//
//        ethRegistrationService = GlobalScope.launch {
//            integrationHelper.runEthRegistrationService(ethRegistrationConfig)
//        }
//        runBlocking { delay(5_000) }
//    }
//
//    @AfterAll
//    fun dropDown() {
//        registrationServiceEnvironment.close()
//        ethRegistrationService.cancel()
//    }

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
        var res = registrationServiceEnvironment.register(name, pubkey)
        assertEquals(200, res.statusCode)
        // register in Eth
        res = integrationHelper.sendRegistrationRequest(
            name,
            whitelist,
            pubkey,
            ethRegistrationConfig.port
        )
        assertEquals(200, res.statusCode)

        // check relay address
        assertEquals(freeRelay, integrationHelper.getRelayByAccount(clientId).get())
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
        var res = registrationServiceEnvironment.register(name, pubkey)
        assertEquals(200, res.statusCode)
        // register in eth
        res = integrationHelper.sendRegistrationRequest(
            name,
            whitelist,
            pubkey,
            ethRegistrationConfig.port
        )
        assertEquals(200, res.statusCode)

        // check relay address
        assertEquals(freeRelay, integrationHelper.getRelayByAccount(clientId).get())
        // check whitelist
        assert(integrationHelper.isWhitelisted(clientId, whitelist))


        // deploy free relay
        integrationHelper.deployRelays(1)
        val anotherWhitelist = "0x0000000000000000000000000000000000000123"
        // try to register with the same name
        res = integrationHelper.sendRegistrationRequest(
            name,
            whitelist,
            pubkey,
            ethRegistrationConfig.port
        )
        assertEquals(500, res.statusCode)

        // check relay address the same
        assertEquals(freeRelay, integrationHelper.getRelayByAccount(clientId).get())
        // check whitelist the same
        assert(integrationHelper.isWhitelisted(clientId, whitelist))
        assert(!integrationHelper.isWhitelisted(clientId, anotherWhitelist))
    }

    @Test
    fun ttt1() {
        val whitelist = listOf("0x6826d84158e516f631bBf14586a9BE7e255b2D20", "0x6826d84158e516f631bBf14586a9BE7e255b2D23", "xxx")
        val gson = GsonBuilder().create()
        val json = gson.toJson(whitelist).irohaEscape()

        val irohaCredential = IrohaCredential(
                "ddd@d3",
                ModelUtil.loadKeypair(
                        "/Users/yex/Downloads/ddd@d3.pub",
                        "/Users/yex/Downloads/ddd@d3.priv"
                ).get()
        )
        val ic = IrohaConsumerImpl(irohaCredential, integrationHelper.irohaAPI)

        println("send to $json")
        ModelUtil.setAccountDetail(ic, "ddd@d3", "eth_whitelist", json, ModelUtil.getCurrentTime().toLong(), 2)
                .get()
    }

    @Test
    fun ttt() {
        val irohaCredential = IrohaCredential(
                "qw@d3",
                ModelUtil.loadKeypair(
                        "/Users/yex/Downloads/qw@d3.pub",
                        "/Users/yex/Downloads/qw@d3.priv"
                ).get()
        )
        val ic = IrohaConsumerImpl(irohaCredential, integrationHelper.irohaAPI)

        ModelUtil.transferAssetIroha(
                ic, "qw@d3", "notary@notary", "ether#ethereum", "\" \\ \" \\",
                "0.001", ModelUtil.getCurrentTime().toLong(), 2
        ).get()
    }

}
