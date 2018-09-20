package integration.eth

import integration.helper.IntegrationHelperUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import provider.eth.EthTokenInfo
import util.getRandomString

/**
 * Test Iroha Ethereum ERC20 tokens provider
 */
class EthTokensProviderTest {
    private val integrationHelper = IntegrationHelperUtil()

    private val ethTokensProvider = integrationHelper.ethTokensProvider

    /**
     * Test US-001 Listing all the tokens from tokensProvider
     * Note: Iroha must be deployed to pass the test.
     * @given Iroha network is running and master account added few tokens
     * @when tokensProvider lists all the tokens
     * @then returned list must contain all previously added tokens
     */
    @Disabled
    @Test
    fun testGetTokens() {
        val tokensToAdd = 3
        val expectedTokens = mutableMapOf<String, EthTokenInfo>()
        (1..tokensToAdd).forEach { i ->
            val ethWallet = "0x$i"
            val tokenInfo = EthTokenInfo(String.getRandomString(9), i.toShort())
            expectedTokens[ethWallet] = tokenInfo
            integrationHelper.addERC20Token(ethWallet, tokenInfo.name, tokenInfo.precision)
        }
        ethTokensProvider.getTokens()
            .fold(
                { tokens ->
                    assertFalse(tokens.isEmpty())
                    expectedTokens.forEach { (expectedEthWallet, expectedTokenInfo) ->
                        assertEquals(expectedTokenInfo, tokens.get(expectedEthWallet))
                    }
                },
                { ex -> fail("Cannot get tokens", ex) })
    }
}
