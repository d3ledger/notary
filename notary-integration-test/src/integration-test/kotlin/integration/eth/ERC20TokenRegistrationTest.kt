package integration.eth

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import com.google.gson.Gson
import integration.helper.IntegrationHelperUtil
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import provider.eth.EthTokensProviderImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import token.EthTokenInfo
import token.executeTokenRegistration
import util.getRandomString
import java.io.File

/**
 * Requires Iroha is running
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ERC20TokenRegistrationTest {
    private val integrationHelper = IntegrationHelperUtil()
    private val tokensFilePath = "tokens-for-test.json"
    private val tokenRegistrationConfig =
        integrationHelper.configHelper.createERC20TokenRegistrationConfig(tokensFilePath)

    val irohaNetwork = IrohaNetworkImpl(tokenRegistrationConfig.iroha.hostname, tokenRegistrationConfig.iroha.port)

    private val ethTokensProvider = EthTokensProviderImpl(
        integrationHelper.testCredential,
        irohaNetwork,
        tokenRegistrationConfig.tokenStorageAccount,
        tokenRegistrationConfig.irohaCredential.accountId
    )

    @AfterEach
    fun clearFile() {
        File(tokensFilePath).delete()
    }

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
        irohaNetwork.close()
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
        val tokens = createRandomTokens()
        createTokensFile(tokens, tokensFilePath)
        executeTokenRegistration(tokenRegistrationConfig)
        ethTokensProvider.getTokens()
            .map { tokensFromProvider ->
                val expected = tokensFromProvider
                    .map { (ethAddress, name) ->
                        Pair(ethAddress, EthTokenInfo(name, ethTokensProvider.getTokenPrecision(name).get()))
                    }.sortedBy { it.first }

                assertEquals(
                    tokens.toList().sortedBy { it.first },
                    expected
                )
            }
            .failure { ex -> fail("cannot fetch tokens", ex) }
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
        createTokensFile(HashMap(), tokensFilePath)
        executeTokenRegistration(tokenRegistrationConfig)
        ethTokensProvider.getTokens().fold({ tokensFromProvider ->
            assertTrue(tokensFromProvider.isEmpty())
        }, { ex -> fail("cannot fetch tokens", ex) })
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
    private fun createRandomTokens(): Map<String, EthTokenInfo> {
        val tokensToCreate = 5
        val defaultPrecision = 15
        val tokens = HashMap<String, EthTokenInfo>()
        for (i in 1..tokensToCreate) {
            val tokenName = String.getRandomString(9)
            val tokenInfo = EthTokenInfo(tokenName, defaultPrecision.toShort())
            val tokenAddress = String.getRandomString(16)
            tokens.put(tokenAddress, tokenInfo)
        }
        return tokens
    }
}
