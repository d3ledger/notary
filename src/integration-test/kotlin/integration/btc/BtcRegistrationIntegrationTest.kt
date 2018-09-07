package integration.btc

import integration.helper.IntegrationHelperUtil
import jp.co.soramitsu.iroha.ModelCrypto
import org.bitcoinj.wallet.Wallet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import provider.btc.BtcAddressesProvider
import registration.btc.executeRegistration
import util.getRandomString
import java.io.File
import java.math.BigInteger

class BtcRegistrationIntegrationTest {

    private val integrationHelper = IntegrationHelperUtil()

    /**
     * Test US-001 Client registration
     * Note: Iroha must be deployed to pass the test.
     * @given new client
     * @when client name is passed to registration service
     * @then client has btc address in related Iroha account details and btc address is watched by wallet
     */
    @Disabled
    @Test
    fun testRegistration() {
        val config = integrationHelper.configHelper.createBtcRegistrationConfig()
        executeRegistration(config)
        Thread.sleep(10_000)
        val keypair = ModelCrypto().generateKeypair()
        val userName = String.getRandomString(9)
        val res = khttp.post(
            "http://127.0.0.1:${config.port}/users",
            data = mapOf("name" to userName, "pubkey" to keypair.publicKey().hex())
        )
        assertEquals(200, res.statusCode)
        val registeredBtcAddress = String(res.content)
        val btcAddressesProvider = BtcAddressesProvider(
            config.iroha,
            integrationHelper.irohaKeyPair,
            config.notaryIrohaAccount
        )
        btcAddressesProvider.getAddresses().fold({ addresses ->
            assertEquals("$userName@notary", addresses[registeredBtcAddress])
        }, { ex -> fail(ex) })
        assertEquals(BigInteger.ZERO, integrationHelper.getIrohaAccountBalance("$userName@notary", "btc#bitcoin"))
        val wallet = Wallet.loadFromFile(File(config.btcWalletPath))
        assertNotNull(wallet.issuedReceiveAddresses.find { address -> address.toBase58() == registeredBtcAddress })
    }

}
