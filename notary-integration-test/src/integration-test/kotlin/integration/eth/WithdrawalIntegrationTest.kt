package integration.eth

import com.squareup.moshi.Moshi
import integration.helper.IntegrationHelperUtil
import notary.endpoint.eth.BigIntegerMoshiAdapter
import notary.endpoint.eth.EthNotaryResponse
import notary.endpoint.eth.EthNotaryResponseMoshiAdapter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import provider.eth.ETH_PRECISION
import sidechain.eth.util.DeployHelper
import sidechain.eth.util.hashToWithdraw
import sidechain.eth.util.signUserData
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Class for Ethereum sidechain infrastructure deployment and communication.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WithdrawalIntegrationTest {

    /** Integration tests util */
    private val integrationHelper = IntegrationHelperUtil()

    /** Test Notary configuration */
    private val notaryConfig = integrationHelper.configHelper.createEthNotaryConfig()

    init {
        integrationHelper.runEthNotary(notaryConfig)
    }

    /** Ethereum private key **/
    private val keypair = DeployHelper(
        integrationHelper.configHelper.testConfig.ethereum,
        integrationHelper.configHelper.ethPasswordConfig
    ).credentials.ecKeyPair

    /**
     * Test US-003 Withdrawal of ETH token
     * Note: Iroha must be deployed to pass the test.
     * @given Iroha networks running and user has sent 64203 Ether to master
     * @when withdrawal service queries notary
     * @then notary replies with refund information and signature
     */
    @Test
    fun testRefund() {
        val masterAccount = notaryConfig.notaryCredential.accountId
        val amount = "64203"
        val decimalAmount = BigDecimal(amount).scaleByPowerOfTen(ETH_PRECISION.toInt()).toPlainString()
        val assetId = "ether#ethereum"
        val ethWallet = "eth_wallet"

        // create
        val client = integrationHelper.createClientAccount()
        integrationHelper.addIrohaAssetTo(client.accountId, assetId, decimalAmount)
        integrationHelper.setWhitelist(client.accountId, listOf("0x123", ethWallet))

        // transfer assets from user to notary master account
        val hash = integrationHelper.transferAssetIrohaFromClient(
            client.accountId,
            client.keyPair,
            client.accountId,
            masterAccount,
            assetId,
            ethWallet,
            amount
        )

        // query
        val res =
            khttp.get("http://127.0.0.1:${notaryConfig.refund.port}/${notaryConfig.refund.endpointEthereum}/$hash")

        val moshi = Moshi
            .Builder()
            .add(EthNotaryResponseMoshiAdapter())
            .add(BigInteger::class.java, BigIntegerMoshiAdapter())
            .build()!!
        val ethNotaryAdapter = moshi.adapter(EthNotaryResponse::class.java)!!
        val response = ethNotaryAdapter.fromJson(res.jsonObject.toString())

        assert(response is EthNotaryResponse.Successful)
        response as EthNotaryResponse.Successful

        assertEquals(decimalAmount, response.ethRefund.amount)
        assertEquals(ethWallet, response.ethRefund.address)
        assertEquals("0x0000000000000000000000000000000000000000", response.ethRefund.assetId)

        assertEquals(
            signUserData(
                keypair,
                hashToWithdraw(
                    "0x0000000000000000000000000000000000000000",
                    decimalAmount,
                    ethWallet,
                    hash
                )
            ), response.ethSignature
        )
    }

}
