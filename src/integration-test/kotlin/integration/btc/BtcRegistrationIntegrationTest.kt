package integration.btc

import integration.helper.IntegrationHelperUtil
import jp.co.soramitsu.iroha.ModelCrypto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import provider.btc.BtcTakenAddressesProvider
import registration.btc.executeRegistration
import util.getRandomString
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcRegistrationIntegrationTest {

    private val integrationHelper = IntegrationHelperUtil()

    private val config = integrationHelper.configHelper.createBtcRegistrationConfig()

    init {
        executeRegistration(config)
        Thread.sleep(10_000)
    }

    private val btcTakenAddressesProvider = BtcTakenAddressesProvider(
        config.iroha,
        integrationHelper.irohaKeyPair,
        config.registrationAccount,
        config.iroha.creator
    )

    /**
     * Test US-001 Client registration
     * Note: Iroha must be deployed to pass the test.
     * @given new client
     * @when client name is passed to registration service
     * @then client has btc address in related Iroha account details
     */
    @Disabled
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
        btcTakenAddressesProvider.getTakenAddresses().fold({ addresses ->
            assertEquals("$userName@notary", addresses[registeredBtcAddress])
        }, { ex -> fail("cannot get addresses", ex) })
        assertEquals(
            BigInteger.ZERO,
            integrationHelper.getIrohaAccountBalance("$userName@notary", "btc#bitcoin")
        )
    }

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
            btcTakenAddressesProvider.getTakenAddresses().fold({ addresses ->
                assertEquals("$userName@notary", addresses[registeredBtcAddress])
            }, { ex -> fail("cannot get addresses", ex) })
            assertEquals(
                BigInteger.ZERO,
                integrationHelper.getIrohaAccountBalance("$userName@notary", "btc#bitcoin")
            )
        }
    }

    @Test
    fun testRegistrationNoFree() {
        val clientsBeforeRegistration = btcTakenAddressesProvider.getTakenAddresses().get().size
        val keypair = ModelCrypto().generateKeypair()
        val userName = String.getRandomString(9)
        val res = khttp.post(
            "http://127.0.0.1:${config.port}/users",
            data = mapOf("name" to userName, "pubkey" to keypair.publicKey().hex())
        )
        assertEquals(400, res.statusCode)
        assertEquals(clientsBeforeRegistration, btcTakenAddressesProvider.getTakenAddresses().get().size)

    }
}
