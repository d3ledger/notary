package notary

import com.github.kittinunf.result.Result
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.mock
import endpoint.eth.EthRefundRequest
import endpoint.eth.EthRefundStrategy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.Assertions.assertEquals

class NotaryInitializationTest {

    /** Some exception message */
    val throwMessage = "Hey, look at my exception!"

    /**
     * @given eth wallets loader and notary initializer
     * @when eth wallets loader fails with exception
     * @then notary initializer returns Result with reason
     */
    @Test
    fun testFailToLoadWallets() {

        // Wallet provider that fails with exception
        val failEthWalletProvider = mock<EthWalletsProvider> {
            on {
                getWallets()
            } doReturn Result.of { throw Exception(throwMessage) }
        }

        val notaryInit = NotaryInitialization(failEthWalletProvider)

        notaryInit.init().fold(
            { fail { "Exception should be thrown in wallets loader" } },
            { exc ->
                assertEquals(throwMessage, exc.message)
            }
        )
    }

    /**
     * @given eth tokens loader and notary initializer
     * @when eth tokens loader fails with exception
     * @then notary initializer returns Result with reason
     */
    @Test
    fun testFailToLoadTokens() {

        // Wallet provider
        val ethWalletProvider = mock<EthWalletsProvider> {
            on {
                getWallets()
            } doReturn Result.of { mapOf<String, String>() }
        }

        // Token provider that fails with exception
        val failEthTokenProvider = mock<EthTokensProvider> {
            on {
                getTokens()
            } doReturn Result.of { throw Exception(throwMessage) }
        }

        val notaryInit = NotaryInitialization(ethWalletProvider, failEthTokenProvider)

        notaryInit.init().fold(
            { fail { "Exception should be thrown in tokens loader" } },
            { exc ->
                assertEquals(throwMessage, exc.message)
            }
        )
    }
}
