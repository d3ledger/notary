package integration.eth

import com.squareup.moshi.Moshi
import integration.helper.EthIntegrationHelperUtil
import integration.helper.IrohaConfigHelper
import com.d3.eth.deposit.endpoint.BigIntegerMoshiAdapter
import com.d3.eth.deposit.endpoint.EthNotaryResponse
import com.d3.eth.deposit.endpoint.EthNotaryResponseMoshiAdapter
import com.d3.eth.deposit.ENDPOINT_ETHEREUM
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import com.d3.eth.provider.ETH_PRECISION
import com.d3.eth.provider.EthRelayProviderIrohaImpl
import com.d3.eth.sidechain.util.DeployHelper
import com.d3.eth.sidechain.util.hashToWithdraw
import com.d3.eth.sidechain.util.signUserData
import com.d3.commons.sidechain.iroha.CLIENT_DOMAIN
import com.d3.commons.util.getRandomString
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration

/**
 * Class for Ethereum sidechain infrastructure deployment and communication.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WithdrawalIntegrationTest {

    /** Integration tests util */
    private val integrationHelper = EthIntegrationHelperUtil()

    /** Test Deposit configuration */
    private val depositConfig = integrationHelper.configHelper.createEthDepositConfig()

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    init {
        integrationHelper.runEthDeposit(ethDepositConfig = depositConfig)
    }

    /** Ethereum private key **/
    private val keypair = DeployHelper(
        depositConfig.ethereum,
        integrationHelper.configHelper.ethPasswordConfig
    ).credentials.ecKeyPair

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
    }

    /**
     * Test US-003 Withdrawal of ETH token
     * Note: Iroha must be deployed to pass the test.
     * @given Iroha networks running and user has sent 64203 Ether to master
     * @when withdrawal service queries notary
     * @then notary replies with refund information and signature
     */
    @Test
    fun testRefund() {
        assertTimeoutPreemptively(timeoutDuration) {
            val masterAccount = depositConfig.notaryCredential.accountId
            val amount = "64203"
            val decimalAmount = BigDecimal(amount).scaleByPowerOfTen(ETH_PRECISION)
            val assetId = "ether#ethereum"
            val ethWallet = "0x1334"

            // create
            val client = String.getRandomString(9)
            val clientId = "$client@$CLIENT_DOMAIN"
            integrationHelper.registerClient(client, listOf(ethWallet), integrationHelper.testCredential.keyPair)
            integrationHelper.addIrohaAssetTo(clientId, assetId, decimalAmount)
            val relay = EthRelayProviderIrohaImpl(
                integrationHelper.queryAPI,
                masterAccount,
                integrationHelper.accountHelper.registrationAccount.accountId
            ).getRelays().get().filter {
                it.value == clientId
            }.keys.first()

            // transfer assets from user to notary master account
            val hash = integrationHelper.transferAssetIrohaFromClient(
                clientId,
                integrationHelper.testCredential.keyPair,
                clientId,
                masterAccount,
                assetId,
                ethWallet,
                amount
            )

            // query
            val res =
                khttp.get("http://127.0.0.1:${depositConfig.refund.port}/$ENDPOINT_ETHEREUM/$hash")

            val moshi = Moshi
                .Builder()
                .add(EthNotaryResponseMoshiAdapter())
                .add(BigInteger::class.java, BigIntegerMoshiAdapter())
                .build()!!
            val ethNotaryAdapter = moshi.adapter(EthNotaryResponse::class.java)!!
            val response = ethNotaryAdapter.fromJson(res.jsonObject.toString())

            assert(response is EthNotaryResponse.Successful)
            response as EthNotaryResponse.Successful

            assertEquals(decimalAmount.toPlainString(), response.ethRefund.amount)
            assertEquals(ethWallet, response.ethRefund.address)
            assertEquals("0x0000000000000000000000000000000000000000", response.ethRefund.assetId)

            assertEquals(
                signUserData(
                    keypair,
                    hashToWithdraw(
                        "0x0000000000000000000000000000000000000000",
                        decimalAmount.toPlainString(),
                        ethWallet,
                        hash,
                        relay
                    )
                ), response.ethSignature
            )
        }
    }
}
