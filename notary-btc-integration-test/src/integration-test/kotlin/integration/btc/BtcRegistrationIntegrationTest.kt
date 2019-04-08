package integration.btc

import com.d3.commons.sidechain.iroha.CLIENT_DOMAIN
import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import com.squareup.moshi.Moshi
import integration.btc.environment.BtcRegistrationTestEnvironment
import integration.helper.BtcIntegrationHelperUtil
import integration.registration.RegistrationServiceTestEnvironment
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcRegistrationIntegrationTest {

    private val integrationHelper = BtcIntegrationHelperUtil()

    private val btcRegistrationEnvironment = BtcRegistrationTestEnvironment(integrationHelper)
    private val registrationServiceEnvironment = RegistrationServiceTestEnvironment(integrationHelper)

    // Moshi adapter for response JSON deserialization
    private val moshiAdapter = Moshi
        .Builder()
        .build()!!.adapter(Map::class.java)!!

    init {
        registrationServiceEnvironment.registrationInitialization.init()
        btcRegistrationEnvironment.btcRegistrationServiceInitialization.init()
        runBlocking { delay(5_000) }
    }

    @AfterAll
    fun dropDown() {
        registrationServiceEnvironment.close()
        btcRegistrationEnvironment.close()
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given new client
     * @when client name is passed to registration service
     * @then client has btc address in related Iroha account details
     */
    @Test
    fun testRegistration() {
        integrationHelper.genFreeBtcAddress(btcRegistrationEnvironment.btcAddressGenerationConfig.btcKeysWalletPath)
        val keypair = Ed25519Sha3().generateKeypair()
        val userName = String.getRandomString(9)
        var res = registrationServiceEnvironment.register(userName, keypair.public.toHexString())
        assertEquals(200, res.statusCode)
        res = btcRegistrationEnvironment.register(userName, keypair.public.toHexString())
        assertEquals(200, res.statusCode)

        val response = moshiAdapter.fromJson(res.jsonObject.toString())!!
        val registeredBtcAddress = response["clientId"]

        btcRegistrationEnvironment.btcRegisteredAddressesProvider.getRegisteredAddresses().fold({ addresses ->
            assertEquals(
                "$userName@$CLIENT_DOMAIN",
                addresses.first { btcAddress -> btcAddress.address == registeredBtcAddress }.info.irohaClient
            )
        }, { ex -> fail("cannot get addresses", ex) })
        assertEquals(
            BigInteger.ZERO.toString(),
            integrationHelper.getIrohaAccountBalance("$userName@$CLIENT_DOMAIN", "btc#bitcoin")
        )
    }


    /**
     * Note: Iroha must be deployed to pass the test.
     * @given new client
     * @when client name is passed to registration service twice
     * @then client has btc address in related Iroha account details, second registration attempt fails
     */
    @Test
    fun testDoubleRegistration() {
        integrationHelper.genFreeBtcAddress(btcRegistrationEnvironment.btcAddressGenerationConfig.btcKeysWalletPath)
        val keypair = Ed25519Sha3().generateKeypair()
        val userName = String.getRandomString(9)
        var res = registrationServiceEnvironment.register(userName, keypair.public.toHexString())
        assertEquals(200, res.statusCode)
        res = btcRegistrationEnvironment.register(userName, keypair.public.toHexString())
        assertEquals(200, res.statusCode)
        val response = moshiAdapter.fromJson(res.jsonObject.toString())!!

        //Double registration
        res = btcRegistrationEnvironment.register(userName, keypair.public.toHexString())
        assertEquals(500, res.statusCode)

        val registeredBtcAddress = response["clientId"]

        btcRegistrationEnvironment.btcRegisteredAddressesProvider.getRegisteredAddresses().fold({ addresses ->
            assertEquals(
                "$userName@$CLIENT_DOMAIN",
                addresses.first { btcAddress -> btcAddress.address == registeredBtcAddress }.info.irohaClient
            )
        }, { ex -> fail("cannot get addresses", ex) })
        assertEquals(
            BigInteger.ZERO.toString(),
            integrationHelper.getIrohaAccountBalance("$userName@$CLIENT_DOMAIN", "btc#bitcoin")
        )
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given new client, but no free addresses for my node
     * @when client name is passed to registration service
     * @then client stays unregistered
     */
    @Test
    fun testRegistrationNoAddressForMyNode() {
        val clientsBeforeRegistration =
            btcRegistrationEnvironment.btcRegisteredAddressesProvider.getRegisteredAddresses().get().size
        integrationHelper.genFreeBtcAddress(
            btcRegistrationEnvironment.btcAddressGenerationConfig.btcKeysWalletPath,
            "different node id"
        )
        val keypair = Ed25519Sha3().generateKeypair()
        val userName = String.getRandomString(9)
        var res = registrationServiceEnvironment.register(userName, keypair.public.toHexString())
        assertEquals(200, res.statusCode)
        res = btcRegistrationEnvironment.register(userName, keypair.public.toHexString())

        assertEquals(500, res.statusCode)
        assertEquals(
            clientsBeforeRegistration,
            btcRegistrationEnvironment.btcRegisteredAddressesProvider.getRegisteredAddresses().get().size
        )
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given multiple clients
     * @when client names are passed to registration service
     * @then all the clients have btc address in related Iroha account details
     */
    @Test
    fun testRegistrationMultiple() {
        val takenAddresses = HashSet<String>()
        val addressesToRegister = 3
        integrationHelper.preGenFreeBtcAddresses(
            btcRegistrationEnvironment.btcAddressGenerationConfig.btcKeysWalletPath,
            addressesToRegister
        )
        for (i in 1..addressesToRegister) {
            val num =
                khttp.get("http://127.0.0.1:${btcRegistrationEnvironment.btcRegistrationConfig.port}/free-addresses/number")
            assertEquals((addressesToRegister - i + 1).toString(), num.text)

            val keypair = Ed25519Sha3().generateKeypair()
            val userName = String.getRandomString(9)
            val newestAddress = btcRegistrationEnvironment.btcFreeAddressesProvider.getFreeAddresses().get()
                .maxBy { address -> address.info.generationTime ?: 0 }!!
            var res = registrationServiceEnvironment.register(userName, keypair.public.toHexString())
            assertEquals(200, res.statusCode)
            res = btcRegistrationEnvironment.register(userName, keypair.public.toHexString())
            assertEquals(200, res.statusCode)
            val response = moshiAdapter.fromJson(res.jsonObject.toString())!!
            val registeredBtcAddress = response["clientId"].toString()
            assertEquals(newestAddress.address, registeredBtcAddress)
            assertFalse(takenAddresses.contains(registeredBtcAddress))
            takenAddresses.add(registeredBtcAddress)
            btcRegistrationEnvironment.btcRegisteredAddressesProvider.getRegisteredAddresses().fold({ addresses ->
                assertEquals(
                    "$userName@$CLIENT_DOMAIN",
                    addresses.first { btcAddress -> btcAddress.address == registeredBtcAddress }.info.irohaClient
                )
            }, { ex -> fail("cannot get addresses", ex) })
            assertEquals(
                BigInteger.ZERO.toString(),
                integrationHelper.getIrohaAccountBalance("$userName@$CLIENT_DOMAIN", "btc#bitcoin")
            )
        }

        val num =
            khttp.get("http://127.0.0.1:${btcRegistrationEnvironment.btcRegistrationConfig.port}/free-addresses/number")
        assertEquals("0", num.text)

    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given no registered btc addreses
     * @when client name is passed to registration service
     * @then client stays unregistered
     */
    @Test
    fun testRegistrationNoFree() {
        val clientsBeforeRegistration =
            btcRegistrationEnvironment.btcRegisteredAddressesProvider.getRegisteredAddresses().get().size
        val keypair = Ed25519Sha3().generateKeypair()
        val userName = String.getRandomString(9)

        val num =
            khttp.get("http://127.0.0.1:${btcRegistrationEnvironment.btcRegistrationConfig.port}/free-addresses/number")
        assertEquals("0", num.text)

        var res = registrationServiceEnvironment.register(userName, keypair.public.toHexString())
        assertEquals(200, res.statusCode)
        res = btcRegistrationEnvironment.register(userName, keypair.public.toHexString())
        assertEquals(500, res.statusCode)
        assertEquals(
            clientsBeforeRegistration,
            btcRegistrationEnvironment.btcRegisteredAddressesProvider.getRegisteredAddresses().get().size
        )
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given only one "change" address is generated
     * @when client name is passed to registration service
     * @then client stays unregistered, because there are no "free" Bitcoin addresses
     */
    @Test
    fun testRegistrationOnlyChangeAddresses() {
        val clientsBeforeRegistration =
            btcRegistrationEnvironment.btcRegisteredAddressesProvider.getRegisteredAddresses().get().size
        integrationHelper.genChangeBtcAddress(btcRegistrationEnvironment.btcAddressGenerationConfig.btcKeysWalletPath)

        val num =
            khttp.get("http://127.0.0.1:${btcRegistrationEnvironment.btcRegistrationConfig.port}/free-addresses/number")
        assertEquals("0", num.text)

        val keypair = Ed25519Sha3().generateKeypair()
        val userName = String.getRandomString(9)
        var res = registrationServiceEnvironment.register(userName, keypair.public.toHexString())
        assertEquals(200, res.statusCode)
        res = btcRegistrationEnvironment.register(userName, keypair.public.toHexString())
        assertEquals(500, res.statusCode)
        assertEquals(
            clientsBeforeRegistration,
            btcRegistrationEnvironment.btcRegisteredAddressesProvider.getRegisteredAddresses().get().size
        )
    }
}
