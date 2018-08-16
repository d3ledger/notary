package integration.eth

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.success
import config.TestConfig
import config.loadConfigs
import jp.co.soramitsu.iroha.Keypair
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import provider.eth.EthFreeRelayProvider
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil.loadKeypair
import sidechain.iroha.util.ModelUtil.setAccountDetail

class EthFreeRelayProviderTest {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure {
                println(it)
                System.exit(1)
            }
    }

    /** Test configurations */
    val testConfig = loadConfigs("test", TestConfig::class.java)

    /** Iroha keypair */
    val keypair: Keypair =
        loadKeypair(
            testConfig.iroha.pubkeyPath,
            testConfig.iroha.privkeyPath
        ).get()

    /** Iroha consumer */
    val irohaConsumer = IrohaConsumerImpl(testConfig.iroha)

    /** Iroha transaction creator */
    val creator = testConfig.iroha.creator

    /**
     * @given Iroha network running and Iroha master account with attribute ["eth_wallet", "free"] set by master account
     * @when getRelay() of FreeRelayProvider is called
     * @then "eth_wallet" attribute key is returned
     */
    @Test
    fun getFreeWallet() {
        val ethFreeWallet = "eth_free_wallet_stub"

        setAccountDetail(irohaConsumer, creator, testConfig.notaryIrohaAccount, ethFreeWallet, "free")
            .failure { fail(it) }

        val freeWalletsProvider =
            EthFreeRelayProvider(
                testConfig.iroha,
                keypair,
                testConfig.notaryIrohaAccount,
                creator
            )
        val result = freeWalletsProvider.getRelay()

        assertEquals(ethFreeWallet, result.get())
    }

    /**
     * @given Iroha network running and Iroha master account
     * @when getRelay() of FreeRelayProvider is called with wrong master account
     * @then "eth_wallet" attribute key is returned
     */
    @Test
    fun getFreeWalletException() {
        val wrongMasterAccount = "wrong@account"

        val freeWalletsProvider =
            EthFreeRelayProvider(testConfig.iroha, keypair, creator, wrongMasterAccount)
        freeWalletsProvider.getRelay()
            .success { fail { "should return Exception" } }
    }
}
