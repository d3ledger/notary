package integration.eth

import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil.setAccountDetail
import com.d3.eth.provider.EthFreeRelayProvider
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.success
import integration.helper.EthIntegrationHelperUtil
import integration.helper.IrohaConfigHelper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import java.io.File
import java.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EthFreeRelayProviderTest {

    /** Test configurations */
    val integrationHelper = EthIntegrationHelperUtil()

    /** Iroha consumer */
    private val irohaConsumer =
        IrohaConsumerImpl(integrationHelper.testCredential, integrationHelper.irohaAPI)

    /** Iroha transaction creator */
    val creator = integrationHelper.testCredential.accountId

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
    }

    /**
     * @given Iroha network running and Iroha master account with attribute ["eth_wallet", "free"] set by master account
     * @when getRelay() of FreeRelayProvider is called
     * @then "eth_wallet" attribute key is returned
     */
    @Test
    fun getFreeWallet() {
        assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            val ethFreeWallet = "eth_free_wallet_stub"

            setAccountDetail(
                irohaConsumer,
                integrationHelper.accountHelper.notaryAccount.accountId,
                ethFreeWallet,
                "free"
            )
                .failure { fail(it) }

            val freeWalletsProvider =
                EthFreeRelayProvider(
                    integrationHelper.queryHelper,
                    integrationHelper.accountHelper.notaryAccount.accountId,
                    creator
                )
            val result = freeWalletsProvider.getRelay()

            assertEquals(ethFreeWallet, result.get())
        }
    }

    /**
     * @given Iroha network running and Iroha master account
     * @when getRelay() of FreeRelayProvider is called with wrong master account
     * @then "eth_wallet" attribute key is returned
     */
    @Test
    fun getFreeWalletException() {
        val wrongMasterAccount = "wrong@account"

        val freeWalletsProvider =
            EthFreeRelayProvider(integrationHelper.queryHelper, creator, wrongMasterAccount)
        freeWalletsProvider.getRelay()
            .success { fail { "should return Exception" } }
    }

    /**
     * @given Iroha network running, file with relay addresses is created
     * @when Relays are imported from the file
     * @then Free wallet provider returns same relays as in the file
     */
    @Test
    fun testStorageFromFile() {
        assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            val relayHolder = File.createTempFile("relay", "free")
            relayHolder.deleteOnExit()
            val existingRelays = setOf(
                "0x6fab8fe8e0e5f4a3e8b2ff794e023fd359137f35",
                "0x2d864560b9b48c99c633427c67020c8f883be360",
                "0x20d0b61725d279c0e4fd73e059ab163f2aea0761"
            )
            existingRelays.map { relayHolder.appendText(it + System.lineSeparator()) }

            integrationHelper.importRelays(relayHolder.absolutePath)

            val freeWalletsProvider =
                EthFreeRelayProvider(
                    integrationHelper.queryHelper,
                    integrationHelper.accountHelper.notaryAccount.accountId,
                    integrationHelper.accountHelper.registrationAccount.accountId
                )

            freeWalletsProvider.getRelays()
                .fold(
                    { assertEquals(existingRelays, it) },
                    { ex -> fail("result has exception", ex) }
                )
        }
    }
}
