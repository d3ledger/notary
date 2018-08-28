package integration

import com.squareup.moshi.Moshi
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
import java.math.BigInteger

/**
 * Class for Ethereum sidechain infrastructure deployment and communication.
 */
class WithdrawalIntegrationTest {

    /** Integration tests util */
    private val integrationHelper = IntegrationHelperUtil()

    /** Iroha transaction creator */
    private val creator = integrationHelper.testConfig.iroha.creator

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

        val masterAccount = integrationHelper.testConfig.notaryIrohaAccount
        val amount = "64203"
        val assetId = "ether#ethereum"
        val ethWallet = "eth_wallet"

        // add assets to user
        integrationHelper.addIrohaAssetTo(creator, assetId, amount)

        integrationHelper.setWhitelist(creator, listOf("0x123", ethWallet))

        // transfer assets from user to notary master account
        val hash = integrationHelper.transferAssetIrohaFromClient(
            creator,
            integrationHelper.irohaKeyPair,
            creator,
            masterAccount,
            assetId,
            ethWallet,
            amount
        )

        // query
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
                integrationHelper.testConfig.ethereum,
                integrationHelper.passwordConfig,
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
