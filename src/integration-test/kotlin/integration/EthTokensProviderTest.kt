package integration

import com.github.kittinunf.result.failure
import config.TestConfig
import config.loadConfigs
import integration.helper.IntegrationHelperUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import provider.EthTokensProviderImpl
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

/**
 * Test Iroha Ethereum ERC20 tokens provider
 */
class EthTokensProviderTest {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure { ex ->
                ex.printStackTrace()
                System.exit(1)
            }
    }

    private val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties")
    private val integrationHelper = IntegrationHelperUtil()

    /** Iroha keypair */
    private val irohaKeyPair = ModelUtil.loadKeypair(testConfig.iroha.pubkeyPath, testConfig.iroha.privkeyPath).get()

    private val ethTokensProvider = EthTokensProviderImpl(
        testConfig.iroha,
        irohaKeyPair,
        testConfig.notaryIrohaAccount,
        integrationHelper.dataSetterAccount
    )

    /**
     * Test US-001 Listing all the tokens from tokensProvider
     * Note: Iroha must be deployed to pass the test.
     * @given Iroha network is running and master account added few tokens
     * @when tokensProvider lists all the tokens
     * @then returned list must contain all previously added tokens
     */
    @Test
    fun testGetTokens() {
        val tokensToAdd = 5
        (1..tokensToAdd).forEach { i ->
            ethTokensProvider.addToken("0x$i", "$i")
        }
        ethTokensProvider.getTokens()
            .fold(
                { tokens ->
                    assertFalse(tokens.isEmpty())
                    (1..tokensToAdd).forEach { i ->
                        assertEquals("$i", tokens.get("0x$i"))
                    }
                },
                { ex -> fail("Cannot get tokens", ex) })
    }
    /**
     * Test US-002 Adding tokens using tokensProvider
     * Note: Iroha must be deployed to pass the test.
     * @given Iroha network is running
     * @when tokensProvider adds a token
     * @then tokens list must contain previously added token and also its size must be increased by one
     */
    @Test
    fun testAddToken() {
        val initialNumberOfTokens = ethTokensProvider.getTokens().get().size
        val newEthWallet = "0x123"
        val newTokenName = "abc"
        ethTokensProvider.addToken(newEthWallet, newTokenName).failure { ex -> fail("Cannot add token", ex) }
        val tokens = ethTokensProvider.getTokens().get()
        assertEquals(initialNumberOfTokens + 1, tokens.size)
        assertEquals(newTokenName, tokens.get(newEthWallet))
    }
}
