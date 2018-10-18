package integration.eth

import com.github.kittinunf.result.success
import integration.helper.IntegrationHelperUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import provider.eth.ETH_ADDRESS
import provider.eth.ETH_NAME
import provider.eth.ETH_PRECISION
import token.EthTokenInfo
import util.getRandomString

/**
 * Test Iroha Ethereum ERC20 tokens provider
 */
@Disabled
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
    @Test
    fun testGetTokens() {
        val tokensToAdd = 3
        val expectedTokens = mutableMapOf<String, EthTokenInfo>()
        (1..tokensToAdd).forEach { i ->
            val ethWallet = "0x$i"
            val tokenName = String.getRandomString(9)
            val tokenPrecision = i.toShort()
            expectedTokens[ethWallet] = EthTokenInfo(tokenName, tokenPrecision)
            integrationHelper.addERC20Token(ethWallet, tokenName, tokenPrecision)
        }
        ethTokensProvider.getTokens()
            .fold(
                { tokens ->
                    assertFalse(tokens.isEmpty())
                    expectedTokens.forEach { (expectedEthWallet, expectedTokenInfo) ->
                        val (expectedName, expectedPrecision) = expectedTokenInfo
                        assertEquals(expectedName, tokens.get(expectedEthWallet))
                        assertEquals(expectedPrecision, ethTokensProvider.getTokenPrecision(expectedName).get())
                        assertEquals(expectedEthWallet, ethTokensProvider.getTokenAddress(expectedName).get())
                    }
                },
                { ex -> fail("Cannot get tokens", ex) })
    }

    /**
     * @given Iroha network is running
     * @when tokenProvider is queried with some nonexistent asset
     * @then failure result is returned
     */
    @Test
    fun getNonexistentToken() {
        ethTokensProvider.getTokenPrecision("nonexist")
            .fold(
                { fail("Result returned success while failure is expected.") },
                { Unit }
            )

        ethTokensProvider.getTokenAddress("nonexist")
            .fold(
                { fail("Result returned success while failure is expected.") },
                { assertEquals("Collection is empty.", it.message) }
            )
    }

    /**
     * Test predefined asset ethereum.
     * @given Iroha network is running
     * @when tokenProvider is queried with "ether"
     * @then predefined parameters are returned
     */
    @Test
    fun getEthereum() {
        ethTokensProvider.getTokenPrecision(ETH_NAME)
            .success { assertEquals(ETH_PRECISION, it) }

        ethTokensProvider.getTokenAddress(ETH_NAME)
            .success { assertEquals(ETH_ADDRESS, it) }
    }
}
