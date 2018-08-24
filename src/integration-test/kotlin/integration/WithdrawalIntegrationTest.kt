package integration

import com.github.kittinunf.result.failure
import com.squareup.moshi.Moshi
import config.EthereumPasswords
import config.TestConfig
import config.loadConfigs
import integration.helper.IntegrationHelperUtil
import kotlinx.coroutines.experimental.async
import notary.endpoint.eth.BigIntegerMoshiAdapter
import notary.endpoint.eth.EthNotaryResponse
import notary.endpoint.eth.EthNotaryResponseMoshiAdapter
import notary.main
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import sidechain.eth.util.hashToWithdraw
import sidechain.eth.util.signUserData
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import java.math.BigInteger

/**
 * Class for Ethereum sidechain infrastructure deployment and communication.
 */
class WithdrawalIntegrationTest {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure {
                println(it)
                System.exit(1)
            }
    }

    /** Configurations for tests */
    val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties")

    /** Ethereum password configs */
    val passwordConfig = loadConfigs("test", EthereumPasswords::class.java, "/ethereum_password.properties")

    /** Iroha network layer */
    val irohaConsumer = IrohaConsumerImpl(testConfig.iroha)

    /** Iroha transaction creator */
    val creator = testConfig.iroha.creator

    /** Integration tests util */
    private val integrationHelper = IntegrationHelperUtil()

    /**
     * Test US-003 Withdrawal of ETH token
     * Note: Iroha must be deployed to pass the test.
     * @given Iroha networks running and user has sent 64203 Wei to master
     * @when withdrawal service queries notary
     * @then notary replies with refund information and signature
     */
    @Test
    fun testRefund() {
        async {
            main(arrayOf())
        }

        val masterAccount = testConfig.notaryIrohaAccount
        val amount = "64203"
        val assetId = "ether#ethereum"
        val ethWallet = "eth_wallet"

        // add assets to user
        ModelUtil.addAssetIroha(irohaConsumer, creator, assetId, amount)

        integrationHelper.setWhitelist(creator, listOf("0x123", ethWallet))

        // transfer assets from user to notary master account
        val hash =
            ModelUtil.transferAssetIroha(irohaConsumer, creator, creator, masterAccount, assetId, ethWallet, amount)
                .get()

        // query
        Thread.sleep(4_000)
        println("send")
        val res = khttp.get("http://127.0.0.1:8080/eth/$hash")

        val moshi = Moshi
            .Builder()
            .add(EthNotaryResponseMoshiAdapter())
            .add(BigInteger::class.java, BigIntegerMoshiAdapter())
            .build()!!
        val ethNotaryAdapter = moshi.adapter(EthNotaryResponse::class.java)!!
        val response = ethNotaryAdapter.fromJson(res.jsonObject.toString())

        assert(response is EthNotaryResponse.Successful)
        response as EthNotaryResponse.Successful

        assertEquals(amount, response.ethRefund.amount)
        assertEquals(ethWallet, response.ethRefund.address)
        assertEquals("ether", response.ethRefund.assetId)

        assertEquals(
            signUserData(
                testConfig.ethereum,
                passwordConfig,
                hashToWithdraw(
                    assetId.split("#")[0],
                    amount,
                    ethWallet,
                    hash
                )
            ), response.ethSignature
        )
    }

}
