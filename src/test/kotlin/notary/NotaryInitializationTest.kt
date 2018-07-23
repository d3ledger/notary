package notary

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import config.DatabaseConfig
import config.EthereumConfig
import config.EthereumPasswords
import config.IrohaConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaNetwork

class NotaryInitializationTest {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure {
                println(it)
                System.exit(1)
            }
    }

    /** Some exception message */
    val throwMessage = "Hey, look at my exception!"

    /** Mock iroha configs */
    val irohaConfig = mock<IrohaConfig> {
        on { hostname } doReturn "localhost"
        on { port } doReturn 50051
        on { creator } doReturn "iroha_creator"
        on { pubkeyPath } doReturn "deploy/iroha/keys/admin@notary.pub"
        on { privkeyPath } doReturn "deploy/iroha/keys/admin@notary.priv"
    }

    /** EthereumPasswords mock */
    val passwordConfig = mock<EthereumPasswords> {}

    /** Ethereum configs */
    val ethereumConfig = mock<EthereumConfig> {
        on { url } doReturn "http://localhost:8545"
    }

    /** Mock database configs */
    val dbConfig = mock<DatabaseConfig>()

    /** Mock notary configs */
    val notaryConfig = mock<NotaryConfig> {
        on { iroha } doReturn irohaConfig
        on { ethereum } doReturn ethereumConfig
        on { db } doReturn dbConfig
    }

    /** Mock Wallet provider */
    val ethWalletProvider = mock<EthWalletsProvider> {
        on {
            getWallets()
        } doReturn Result.of { mapOf<String, String>() }
    }

    /** Mock Token provider */
    val ethTokenProvider = mock<EthTokensProvider>()

    /** Mock Iroha network */
    val irohaNetwork = mock<IrohaNetwork>()

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

        val notaryInit =
            NotaryInitialization(notaryConfig, passwordConfig, failEthWalletProvider, ethTokenProvider, irohaNetwork)

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

        // Token provider that fails with exception
        val failEthTokenProvider = mock<EthTokensProvider> {
            on {
                getTokens()
            } doReturn Result.of { throw Exception(throwMessage) }
        }

        val notaryInit = NotaryInitialization(notaryConfig, passwordConfig, ethWalletProvider, failEthTokenProvider)

        notaryInit.init().fold(
            { fail { "Exception should be thrown in tokens loader" } },
            { exc ->
                assertEquals(throwMessage, exc.message)
            }
        )
    }
}
