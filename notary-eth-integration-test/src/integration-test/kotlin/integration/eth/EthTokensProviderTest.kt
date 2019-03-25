/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.eth

import com.github.kittinunf.result.success
import integration.helper.EthIntegrationHelperUtil
import integration.helper.IrohaConfigHelper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import com.d3.eth.provider.ETH_ADDRESS
import com.d3.eth.provider.ETH_DOMAIN
import com.d3.eth.provider.ETH_NAME
import com.d3.eth.provider.ETH_PRECISION
import com.d3.eth.token.EthTokenInfo
import com.d3.commons.util.getRandomString
import java.time.Duration

/**
 * Test Iroha Ethereum ERC20 tokens provider
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EthTokensProviderTest {
    private val integrationHelper = EthIntegrationHelperUtil()

    private val ethTokensProvider = integrationHelper.ethTokensProvider

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
    }

    /**
     * Test US-001 Listing all the tokens from tokensProvider
     * Note: Iroha must be deployed to pass the test.
     * @given Iroha network is running and master account added few tokens
     * @when tokensProvider lists all the tokens
     * @then returned list must contain all previously added tokens
     */
    @Test
    fun testGetTokens() {
        assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            val tokensToAdd = 3
            val expectedTokens = mutableMapOf<String, EthTokenInfo>()
            (1..tokensToAdd).forEach { precision ->
                val ethWallet = "0x$precision"
                val tokenInfo = EthTokenInfo(String.getRandomString(9), ETH_DOMAIN, precision)
                expectedTokens[ethWallet] = tokenInfo
                integrationHelper.addERC20Token(ethWallet, tokenInfo)
            }
            ethTokensProvider.getTokens()
                .fold(
                    { tokens ->
                        assertFalse(tokens.isEmpty())
                        expectedTokens.forEach { (expectedEthWallet, expectedTokenInfo) ->
                            val expectedName = expectedTokenInfo.name
                            val expectedDomain = expectedTokenInfo.domain
                            val expectedPrecision = expectedTokenInfo.precision
                            val assetId = "$expectedName#$expectedDomain"
                            assertEquals("$expectedName#$expectedDomain", tokens.get(expectedEthWallet))
                            assertEquals(expectedPrecision, ethTokensProvider.getTokenPrecision(assetId).get())
                            assertEquals(expectedEthWallet, ethTokensProvider.getTokenAddress(assetId).get())
                        }
                    },
                    { ex -> fail("Cannot get tokens", ex) })
        }
    }

    /**
     * @given Iroha network is running
     * @when tokenProvider is queried with some nonexistent asset
     * @then failure result is returned
     */
    @Test
    fun getNonexistentToken() {
        assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
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
    }

    /**
     * Test predefined asset ethereum.
     * @given Iroha network is running
     * @when tokenProvider is queried with "ether"
     * @then predefined parameters are returned
     */
    @Test
    fun getEthereum() {
        assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            ethTokensProvider.getTokenPrecision("$ETH_NAME#$ETH_DOMAIN")
                .success { assertEquals(ETH_PRECISION, it) }

            ethTokensProvider.getTokenAddress("$ETH_NAME#$ETH_DOMAIN")
                .success { assertEquals(ETH_ADDRESS, it) }
        }
    }
}
