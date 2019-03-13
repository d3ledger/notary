package integration.eth

import integration.helper.EthIntegrationHelperUtil
import integration.helper.IrohaConfigHelper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import com.d3.eth.provider.EthRelayProviderIrohaImpl
import org.junit.jupiter.api.Assertions.*
import java.time.Duration

/**
 * Requires Iroha is running
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EthRelayProviderIrohaTest {

    val integrationHelper = EthIntegrationHelperUtil()

    /** Iroha account that holds details */
    private val relayStorage = integrationHelper.accountHelper.notaryAccount.accountId

    /** Iroha account that has set details */
    private val relaySetter = integrationHelper.accountHelper.registrationAccount.accountId

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
    }

    /**
     * @given ethereum relay wallets are stored in the system
     * @when getRelays() is called
     * @then not free wallets are returned in a map
     */
    @Test
    fun testStorage() {
        assertTimeoutPreemptively(timeoutDuration) {
            val domain = "notary"

            val entries = mapOf(
                "0x281055afc982d96fab65b3a49cac8b878184cb16" to "user1@$domain",
                "0x6f46cf5569aefa1acc1009290c8e043747172d89" to "user2@$domain",
                "0x90e63c3d53e0ea496845b7a03ec7548b70014a91" to "user3@$domain",
                "0x53d284357ec70ce289d6d64134dfac8e511c8a3d" to "free",
                "0xab7c74abc0c4d48d1bdad5dcb26153fc8780f83e" to "user4@$domain",
                "0xfe9e8709d3215310075d67e3ed32a380ccf451c8" to "free"
            )

            integrationHelper.addRelaysToIroha(entries)

            val valid = entries.filter { it.value != "free" }

            EthRelayProviderIrohaImpl(
                integrationHelper.queryAPI,
                relayStorage,
                relaySetter
            ).getRelays()
                .fold(
                    { assertEquals(valid, it) },
                    { ex -> fail("cannot get relays", ex) }
                )
        }
    }

    /**
     * @given There is no relay accounts registered (we use test accountId as relay holder)
     * @when getRelays() is called
     * @then empty map is returned
     */
    @Test
    fun testEmptyStorage() {
        assertTimeoutPreemptively(timeoutDuration) {
            EthRelayProviderIrohaImpl(
                integrationHelper.queryAPI,
                integrationHelper.testCredential.accountId,
                relaySetter
            ).getRelays()
                .fold(
                    { assert(it.isEmpty()) },
                    { ex -> fail("result has exception", ex) }
                )
        }
    }

    @Test
    fun testGetByAccountNotFound() {
        val res = EthRelayProviderIrohaImpl(
            integrationHelper.queryAPI,
            integrationHelper.testCredential.accountId,
            relaySetter
        ).getRelayByAccountId("nonexist@domain")
        assertFalse(res.get().isPresent)
    }
}
