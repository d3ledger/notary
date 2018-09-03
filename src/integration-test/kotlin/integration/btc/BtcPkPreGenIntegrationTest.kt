package integration.btc

import com.github.kittinunf.result.failure
import config.loadConfigs
import integration.helper.IntegrationHelperUtil
import kotlinx.coroutines.experimental.async
import org.bitcoinj.wallet.Wallet
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import provider.TriggerProvider
import provider.btc.BtcKeyGenSessionProvider
import registration.btc.key.BtcPkPreGenConfig
import registration.btc.key.main
import util.getRandomString
import java.io.File

class BtcPkPreGenIntegrationTest {

    private val integrationHelper = IntegrationHelperUtil()

    /**
     * Test US-001 Public key pregeneration
     * Note: Iroha must be deployed to pass the test.
     * @given session account is created
     * @when special trigger account is triggered
     * @then new public key is added to session account
     */
    @Test
    fun testGenerateKey() {
        async { main(emptyArray()) }
        Thread.sleep(3_000)
        val sessionAccountName = String.getRandomString(9)
        val btcPkPreGenConfig =
            loadConfigs("btc-pk-pregen", BtcPkPreGenConfig::class.java, "/btc/pub_key_pregeneration.properties")
        val triggerProvider = TriggerProvider(
            btcPkPreGenConfig.iroha,
            btcPkPreGenConfig.pkTriggerAccount,
            btcPkPreGenConfig.iroha.creator
        )
        val btcKeyGenSessionProvider = BtcKeyGenSessionProvider(btcPkPreGenConfig.iroha, integrationHelper.irohaKeyPair)
        btcKeyGenSessionProvider.createSession(sessionAccountName).failure { fail { "cannot create session" } }
        triggerProvider.trigger(sessionAccountName)
        Thread.sleep(10_000)
        val details = integrationHelper.getAccountDetails("$sessionAccountName@notary", btcPkPreGenConfig.iroha.creator)
        val pubKey = details.values.iterator().next()
        assertNotNull(pubKey)
        val wallet = Wallet.loadFromFile(File(btcPkPreGenConfig.btcWalletFilePath))
        assertNotNull(wallet.issuedReceiveKeys.find { ecKey -> ecKey.publicKeyAsHex == pubKey })
    }
}
