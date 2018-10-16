package integration.eth

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.success
import integration.helper.IntegrationHelperUtil
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import provider.eth.EthFreeRelayProvider
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil.setAccountDetail

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EthFreeRelayProviderTest {

    /** Test configurations */
    val integrationHelper = IntegrationHelperUtil()

    val testConfig = integrationHelper.configHelper.testConfig

    private val irohaNetwork = IrohaNetworkImpl(testConfig.iroha.hostname, testConfig.iroha.port)

    /** Iroha consumer */
    private val irohaConsumer = IrohaConsumerImpl(integrationHelper.testCredential, irohaNetwork)

    /** Iroha transaction creator */
    val creator = integrationHelper.testCredential.accountId

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
        irohaNetwork.close()
    }

    /**
     * @given Iroha network running and Iroha master account with attribute ["eth_wallet", "free"] set by master account
     * @when getRelay() of FreeRelayProvider is called
     * @then "eth_wallet" attribute key is returned
     */
    @Test
    fun getFreeWallet() {
        val ethFreeWallet = "eth_free_wallet_stub"

        setAccountDetail(irohaConsumer, integrationHelper.accountHelper.notaryAccount.accountId, ethFreeWallet, "free")
            .failure { fail(it) }

        val freeWalletsProvider =
            EthFreeRelayProvider(
                integrationHelper.testCredential,
                integrationHelper.accountHelper.notaryAccount.accountId,
                creator,
                irohaNetwork
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
            EthFreeRelayProvider(integrationHelper.testCredential, creator, wrongMasterAccount, irohaNetwork)
        freeWalletsProvider.getRelay()
            .success { fail { "should return Exception" } }
    }
}
