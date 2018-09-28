package integration.btc

import integration.helper.IntegrationHelperUtil
import jp.co.soramitsu.iroha.ModelCrypto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import provider.btc.BtcRegisteredAddressesProvider
import registration.btc.executeRegistration
import util.getRandomString
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
class BtcRegistrationIntegrationTest {

    private val integrationHelper = IntegrationHelperUtil()

    private val config = integrationHelper.configHelper.createBtcRegistrationConfig()

    init {
        executeRegistration(config)
        Thread.sleep(10_000)
    }

    private val btcTakenAddressesProvider = BtcRegisteredAddressesProvider(
        config.iroha,
        integrationHelper.testCredential,
        config.registrationCredential.accountId,
        integrationHelper.testCredential.accountId
    )

    /**
     * Test US-001 Client registration
     * Note: Iroha must be deployed to pass the test.
     * @given new client
     * @when client name is passed to registration service
     * @then client has btc address in related Iroha account details
     */
    @Test
    fun testRegistration() {
        integrationHelper.preGenBtcAddress()
        val keypair = ModelCrypto().generateKeypair()
        val userName = String.getRandomString(9)
        val res = khttp.post(
            "http://127.0.0.1:${config.port}/users",
            data = mapOf("name" to userName, "pubkey" to keypair.publicKey().hex())
        )
        assertEquals(200, res.statusCode)
        val registeredBtcAddress = String(res.content)
        btcTakenAddressesProvider.getRegisteredAddresses().fold({ addresses ->
            assertEquals("$userName@notary", addresses[registeredBtcAddress])
        }, { ex -> fail("cannot get addresses", ex) })
        assertEquals(
            BigInteger.ZERO.toString(),
            integrationHelper.getIrohaAccountBalance("$userName@notary", "btc#bitcoin")
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
        for (i in 1..addressesToRegister) {
            integrationHelper.preGenBtcAddress()
        }
        for (i in 1..addressesToRegister) {
            val keypair = ModelCrypto().generateKeypair()
            val userName = String.getRandomString(9)
            val res = khttp.post(
                "http://127.0.0.1:${config.port}/users",
                data = mapOf("name" to userName, "pubkey" to keypair.publicKey().hex())
            )
            assertEquals(200, res.statusCode)
            val registeredBtcAddress = String(res.content)
            assertFalse(takenAddresses.contains(registeredBtcAddress))
            takenAddresses.add(registeredBtcAddress)
            btcTakenAddressesProvider.getRegisteredAddresses().fold({ addresses ->
                assertEquals("$userName@notary", addresses[registeredBtcAddress])
            }, { ex -> fail("cannot get addresses", ex) })
            assertEquals(
                BigInteger.ZERO.toString(),
                integrationHelper.getIrohaAccountBalance("$userName@notary", "btc#bitcoin")
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
        val clientsBeforeRegistration = btcTakenAddressesProvider.getRegisteredAddresses().get().size
        val keypair = ModelCrypto().generateKeypair()
        val userName = String.getRandomString(9)
        val res = khttp.post(
            "http://127.0.0.1:${config.port}/users",
            data = mapOf("name" to userName, "pubkey" to keypair.publicKey().hex())
        )
        assertEquals(400, res.statusCode)
        assertEquals(clientsBeforeRegistration, btcTakenAddressesProvider.getRegisteredAddresses().get().size)

    }
}
