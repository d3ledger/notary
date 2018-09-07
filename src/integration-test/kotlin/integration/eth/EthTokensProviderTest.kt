package integration.eth

import com.github.kittinunf.result.failure
import integration.helper.IntegrationHelperUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import provider.eth.EthTokenInfo
import provider.eth.EthTokensProviderImpl
import sidechain.iroha.util.ModelUtil
import util.getRandomString

/**
 * Test Iroha Ethereum ERC20 tokens provider
 */
class EthTokensProviderTest {
    private val integrationHelper = IntegrationHelperUtil()

    private val testConfig = integrationHelper.configHelper.testConfig


    private val ethTokensProvider = EthTokensProviderImpl(
        testConfig.iroha,
        integrationHelper.irohaKeyPair,
        testConfig.notaryIrohaAccount,
        integrationHelper.accountHelper.tokenStorageAccount
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
            ethTokensProvider.addToken("0x$i", EthTokenInfo("$i", 0))
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
        val newEthWallet = String.getRandomString(9)
        val newTokenName = "abc"
        ethTokensProvider.addToken(newEthWallet, EthTokenInfo(newTokenName, 0))
            .failure { ex -> fail("Cannot add token", ex) }
        val tokens = ethTokensProvider.getTokens().get()
        assertEquals(initialNumberOfTokens + 1, tokens.size)
        assertEquals(newTokenName, tokens.get(newEthWallet))
    }
}
