package integration.eth

import com.github.kittinunf.result.failure
import integration.helper.IntegrationHelperUtil
import notary.IrohaCommand
import notary.IrohaTransaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import provider.eth.EthRelayProviderIrohaImpl
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.util.ModelUtil

/**
 * Requires Iroha is running
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EthRelayProviderIrohaTest {

    val integrationHelper = IntegrationHelperUtil()
    val testConfig = integrationHelper.configHelper.testConfig

    /** Creator of txs in Iroha */
    val creator: String = testConfig.iroha.creator

    /** Iroha keypair */
    val keypair = ModelUtil.loadKeypair(
        testConfig.iroha.pubkeyPath,
        testConfig.iroha.privkeyPath
    ).get()

    /** Iroha account that has set details */
    val detailSetter = testConfig.registrationIrohaAccount

    /** Iroha account that holds details */
    val detailHolder = testConfig.notaryIrohaAccount

    /**
     * @given [detailHolder] has ethereum wallets in details
     * @when getRelays() is called
     * @then not free wallets are returned in a map
     */
    @Test
    fun storageTest() {
        val domain = "notary"

        val entries = mapOf(
            "0x281055afc982d96fab65b3a49cac8b878184cb16" to "user1@$domain",
            "0x6f46cf5569aefa1acc1009290c8e043747172d89" to "user2@$domain",
            "0x90e63c3d53e0ea496845b7a03ec7548b70014a91" to "user3@$domain",
            "0x53d284357ec70ce289d6d64134dfac8e511c8a3d" to "free",
            "0xab7c74abc0c4d48d1bdad5dcb26153fc8780f83e" to "user4@$domain",
            "0xfe9e8709d3215310075d67e3ed32a380ccf451c8" to "free"
        )

        val valid = entries.filter { it.value != "free" }

        val masterAccount = testConfig.registrationIrohaAccount

        val irohaOutput = IrohaTransaction(
            creator,
            ModelUtil.getCurrentTime(),
            entries.map {
                // Set ethereum wallet as occupied by user id
                IrohaCommand.CommandSetAccountDetail(
                    masterAccount,
                    it.key,
                    it.value
                )
            }
        )

        val tx = IrohaConverterImpl().convert(irohaOutput)

        IrohaConsumerImpl(testConfig.iroha).sendAndCheck(tx)
            .failure { ex -> fail(ex) }

        EthRelayProviderIrohaImpl(
            testConfig.iroha,
            keypair,
            masterAccount,
            creator
        ).getRelays()
            .fold(
                { assertEquals(valid, it) },
                { ex -> fail("cannot get relays", ex) }
            )
    }

    /**
     * @given There is no account details on [detailHolder]
     * @when getRelays() is called
     * @then empty map is returned
     */
    @Test
    fun testEmptyStorage() {
        EthRelayProviderIrohaImpl(testConfig.iroha, keypair, detailSetter, detailHolder).getRelays()
            .fold(
                {
                    assert(it.isEmpty())
                },
                { ex ->
                    fail("result has exception", ex)
                }
            )
    }
}
