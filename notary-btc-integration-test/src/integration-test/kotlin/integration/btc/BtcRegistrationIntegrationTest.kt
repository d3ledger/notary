package integration.btc

import integration.btc.environment.BtcRegistrationTestEnvironment
import integration.helper.BtcIntegrationHelperUtil
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import sidechain.iroha.CLIENT_DOMAIN
import util.getRandomString
import util.toHexString
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcRegistrationIntegrationTest {

    private val integrationHelper = BtcIntegrationHelperUtil()

    private val environment = BtcRegistrationTestEnvironment(integrationHelper)

    init {
        environment.btcRegistrationServiceInitialization.init()
        runBlocking { delay(5_000) }
    }

    @AfterAll
    fun dropDown() {
        environment.close()
    }

    /**
     * Test US-001 Client registration
     * Note: Iroha must be deployed to pass the test.
     * @given new client
     * @when client name is passed to registration service
     * @then client has btc address in related Iroha account details
     */
    @Test
    fun testRegistration() {
        integrationHelper.genFreeBtcAddress(environment.btcNotaryConfig.bitcoin.walletPath)
        val keypair = Ed25519Sha3().generateKeypair()
        val userName = String.getRandomString(9)
        val res = khttp.post(
            "http://127.0.0.1:${environment.btcRegistrationConfig.port}/users",
            data = mapOf("name" to userName, "pubkey" to keypair.public.toHexString())
        )
        assertEquals(200, res.statusCode)
        val registeredBtcAddress = String(res.content)
        environment.btcTakenAddressesProvider.getRegisteredAddresses().fold({ addresses ->
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
     * Test US-002 Client registration
     * Note: Iroha must be deployed to pass the test.
     * @given multiple clients
     * @when client names are passed to registration service
     * @then all the clients have btc address in related Iroha account details
     */
    @Test
    fun testRegistrationMultiple() {
        val takenAddresses = HashSet<String>()
        val addressesToRegister = 3
        integrationHelper.preGenFreeBtcAddresses(environment.btcNotaryConfig.bitcoin.walletPath, addressesToRegister)
        for (i in 1..addressesToRegister) {
            val keypair = Ed25519Sha3().generateKeypair()
            val userName = String.getRandomString(9)
            val res = khttp.post(
                "http://127.0.0.1:${environment.btcRegistrationConfig.port}/users",
                data = mapOf("name" to userName, "pubkey" to keypair.public.toHexString())
            )
            assertEquals(200, res.statusCode)
            val registeredBtcAddress = String(res.content)
            assertFalse(takenAddresses.contains(registeredBtcAddress))
            takenAddresses.add(registeredBtcAddress)
            environment.btcTakenAddressesProvider.getRegisteredAddresses().fold({ addresses ->
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
    }

    /**
     * Test US-002 Client registration
     * Note: Iroha must be deployed to pass the test.
     * @given no registered btc addreses
     * @when client name is passed to registration service
     * @then client stays unregistered
     */
    @Test
    fun testRegistrationNoFree() {
        val clientsBeforeRegistration = environment.btcTakenAddressesProvider.getRegisteredAddresses().get().size
        val keypair = Ed25519Sha3().generateKeypair()
        val userName = String.getRandomString(9)
        val res = khttp.post(
            "http://127.0.0.1:${environment.btcRegistrationConfig.port}/users",
            data = mapOf("name" to userName, "pubkey" to keypair.public.toHexString())
        )
        assertEquals(500, res.statusCode)
        assertEquals(
            clientsBeforeRegistration,
            environment.btcTakenAddressesProvider.getRegisteredAddresses().get().size
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
        val clientsBeforeRegistration = environment.btcTakenAddressesProvider.getRegisteredAddresses().get().size
        integrationHelper.genChangeBtcAddress(environment.btcNotaryConfig.bitcoin.walletPath)
        val keypair = Ed25519Sha3().generateKeypair()
        val userName = String.getRandomString(9)
        val res = khttp.post(
            "http://127.0.0.1:${environment.btcRegistrationConfig.port}/users",
            data = mapOf("name" to userName, "pubkey" to keypair.public.toHexString())
        )
        assertEquals(500, res.statusCode)
        assertEquals(
            clientsBeforeRegistration,
            environment.btcTakenAddressesProvider.getRegisteredAddresses().get().size
        )
    }

}
