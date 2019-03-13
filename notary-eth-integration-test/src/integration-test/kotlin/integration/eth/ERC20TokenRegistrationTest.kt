package integration.eth

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import com.google.gson.Gson
import integration.helper.EthIntegrationHelperUtil
import integration.helper.IrohaConfigHelper
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import com.d3.eth.provider.ETH_DOMAIN
import com.d3.eth.provider.EthTokensProviderImpl
import com.d3.eth.provider.SORA_DOMAIN
import com.d3.eth.provider.XOR_NAME
import com.d3.eth.token.EthTokenInfo
import com.d3.eth.token.executeTokenRegistration
import com.d3.commons.util.getRandomString
import java.io.File
import java.time.Duration

/**
 * Requires Iroha is running
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ERC20TokenRegistrationTest {
    private val integrationHelper = EthIntegrationHelperUtil()
    private val tokensFilePath = "tokens-for-test.json"
    private val tokenRegistrationConfig =
        integrationHelper.configHelper.createERC20TokenRegistrationConfig(tokensFilePath)

    private val ethTokensProvider = EthTokensProviderImpl(
        integrationHelper.queryAPI,
        tokenRegistrationConfig.tokenStorageAccount,
        tokenRegistrationConfig.irohaCredential.accountId
    )

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    @AfterEach
    fun clearFile() {
        File(tokensFilePath).delete()
    }

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
    }

    /**
     * Test US-001 ERC20 tokens registration
     * Note: Iroha must be deployed to pass the test.
     * @given Iroha network is running and json file full ERC20 tokens exists
     * @when ERC20 tokens registration completes
     * @then all the tokens from file are registered in Iroha
     */
    @Test
    fun testTokenRegistration() {
        assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            val tokens = createRandomTokens()
            createTokensFile(tokens, tokensFilePath)
            executeTokenRegistration(tokenRegistrationConfig)
            ethTokensProvider.getTokens()
                .map { tokensFromProvider ->
                    val actual = tokensFromProvider
                        .map { (ethAddress, tokenId) ->
                            val name = tokenId.split("#").first()
                            val domain = tokenId.split("#").last()
                            Pair(
                                ethAddress,
                                EthTokenInfo(
                                    name,
                                    domain,
                                    ethTokensProvider.getTokenPrecision(tokenId).get()
                                )
                            )
                        }.sortedBy { it.first }

                    // xor token is registered in any case
                    tokens.put("0x0000000000000000000000000000000000000000",
                        EthTokenInfo(XOR_NAME, SORA_DOMAIN, 18)
                    )
                    assertEquals(
                        tokens.toList().sortedBy { it.first },
                        actual
                    )
                }
                .failure { ex -> fail("cannot fetch tokens", ex) }
        }
    }

    /**
     * Test US-002 ERC20 tokens registration
     * Note: Iroha must be deployed to pass the test.
     * @given Iroha network is running and json file with no tokens exists
     * @when ERC20 tokens registration completes
     * @then no tokens are registered in Iroha
     */
    @Test
    fun testTokenRegistrationEmptyTokenFile() {
        assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            createTokensFile(HashMap(), tokensFilePath)
            executeTokenRegistration(tokenRegistrationConfig)
            ethTokensProvider.getTokens().fold({ tokensFromProvider ->
                val expected = mapOf(Pair("0x0000000000000000000000000000000000000000", "$XOR_NAME#$SORA_DOMAIN"))
                assertEquals(expected, tokensFromProvider)
            }, { ex -> fail("cannot fetch tokens", ex) })
        }
    }

    //Creates json file full of ERC20 tokens
    private fun createTokensFile(tokens: Map<String, EthTokenInfo>, tokensFilePath: String) {
        val tokensJson = Gson().toJson(tokens)
        val tokeFile = File(tokensFilePath)
        tokeFile.createNewFile()
        tokeFile.printWriter().use { out ->
            out.use { out.println(tokensJson) }
        }
    }

    //Creates randomly generated tokens as a map (token address -> token info)
    private fun createRandomTokens(): MutableMap<String, EthTokenInfo> {
        val tokensToCreate = 5
        val defaultPrecision = 15
        val tokens = HashMap<String, EthTokenInfo>()
        for (i in 1..tokensToCreate) {
            val tokenName = String.getRandomString(9)
            val tokenInfo = EthTokenInfo(tokenName, ETH_DOMAIN, defaultPrecision)
            val tokenAddress = String.getRandomString(16)
            tokens.put(tokenAddress, tokenInfo)
        }
        return tokens
    }
}
