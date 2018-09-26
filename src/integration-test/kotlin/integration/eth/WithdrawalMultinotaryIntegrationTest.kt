package integration.eth

import com.squareup.moshi.Moshi
import config.loadEthPasswords
import integration.helper.IntegrationHelperUtil
import notary.endpoint.eth.BigIntegerMoshiAdapter
import notary.endpoint.eth.EthNotaryResponse
import notary.endpoint.eth.EthNotaryResponseMoshiAdapter
import notary.eth.EthNotaryConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.crypto.ECKeyPair
import sidechain.eth.util.DeployHelper
import sidechain.eth.util.ETH_PRECISION
import sidechain.eth.util.hashToWithdraw
import sidechain.eth.util.signUserData
import sidechain.iroha.util.ModelUtil
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WithdrawalMultinotaryIntegrationTest {
    /** Utility functions for integration tests */
    private val integrationHelper = IntegrationHelperUtil()

    /** Path to public key of 2nd instance of notary */
    private val pubkeyPath = "deploy/iroha/keys/notary2@notary.pub"

    /** Path to private key of 2nd instance of notary */
    private val privkeyPath = "deploy/iroha/keys/notary2@notary.priv"

    private val notaryConfig1: EthNotaryConfig

    private val notaryConfig2: EthNotaryConfig

    private val keypair1: ECKeyPair

    private val keypair2: ECKeyPair

    private val ethereumPasswords = loadEthPasswords("eth-notary", "/eth/ethereum_password.properties")

    init {
        // create 1st notary config
        val ethereumConfig1 = integrationHelper.configHelper.createEthereumConfig("deploy/ethereum/keys/ganache.key")

        keypair1 = DeployHelper(ethereumConfig1, ethereumPasswords).credentials.ecKeyPair

        // run 1st instance of notary
        notaryConfig1 = integrationHelper.configHelper.createEthNotaryConfig()
        integrationHelper.runEthNotary(notaryConfig1)


        // create 2nd notary config
        val ethereumConfig2 = integrationHelper.configHelper.createEthereumConfig("deploy/ethereum/keys/ganache2.key")
        val irohaConfig2 =
            integrationHelper.configHelper.createIrohaConfig(pubkeyPath = pubkeyPath, privkeyPath = privkeyPath)
        notaryConfig2 = integrationHelper.configHelper.createEthNotaryConfig(irohaConfig2, ethereumConfig2)

        keypair2 = DeployHelper(ethereumConfig2, ethereumPasswords).credentials.ecKeyPair

        integrationHelper.accountHelper.addNotarySignatory(ModelUtil.loadKeypair(pubkeyPath, privkeyPath).get())

        // run 2nd instance of notary
        integrationHelper.runEthNotary(notaryConfig2)
    }

    /**
     * Test US-003 Withdrawal of ETH token
     * Note: Iroha must be deployed to pass the test.
     * @given Iroha is running, 2 notaries are running and user has sent 64203 Ether to master
     * @when withdrawal service queries notary1 and notary2
     * @then both notaries reply with valid refund information and signature
     */
    @Test
    fun testRefund() {
        val masterAccount = integrationHelper.accountHelper.notaryAccount
        val amount = "64203"
        val decimalAmount = BigDecimal(amount).scaleByPowerOfTen(ETH_PRECISION.toInt()).toPlainString()
        val assetId = "ether#ethereum"
        val ethWallet = "eth_wallet"

        // create
        val client = integrationHelper.registerClient()
        integrationHelper.addIrohaAssetTo(client, assetId, decimalAmount)
        integrationHelper.setWhitelist(client, listOf("0x123", ethWallet))

        // transfer assets from user to notary master account
        val hash = integrationHelper.transferAssetIrohaFromClient(
            client,
            integrationHelper.irohaKeyPair,
            client,
            masterAccount,
            assetId,
            ethWallet,
            amount
        )

        // query 1
        val res1 =
            khttp.get("http://127.0.0.1:${notaryConfig1.refund.port}/${notaryConfig1.refund.endpointEthereum}/$hash")

        val moshi = Moshi
            .Builder()
            .add(EthNotaryResponseMoshiAdapter())
            .add(BigInteger::class.java, BigIntegerMoshiAdapter())
            .build()!!
        val ethNotaryAdapter = moshi.adapter(EthNotaryResponse::class.java)!!
        val response1 = ethNotaryAdapter.fromJson(res1.jsonObject.toString())

        assert(response1 is EthNotaryResponse.Successful)
        response1 as EthNotaryResponse.Successful

        Assertions.assertEquals(decimalAmount, response1.ethRefund.amount)
        Assertions.assertEquals(ethWallet, response1.ethRefund.address)
        Assertions.assertEquals("0x0000000000000000000000000000000000000000", response1.ethRefund.assetId)

        Assertions.assertEquals(
            signUserData(
                keypair1,
                hashToWithdraw(
                    "0x0000000000000000000000000000000000000000",
                    decimalAmount,
                    ethWallet,
                    hash,
                    ""
                )
            ), response1.ethSignature
        )

        // query 2
        val res2 =
            khttp.get("http://127.0.0.1:${notaryConfig2.refund.port}/${notaryConfig2.refund.endpointEthereum}/$hash")

        val response2 = ethNotaryAdapter.fromJson(res2.jsonObject.toString())

        assert(response2 is EthNotaryResponse.Successful)
        response2 as EthNotaryResponse.Successful

        Assertions.assertEquals(decimalAmount, response2.ethRefund.amount)
        Assertions.assertEquals(ethWallet, response2.ethRefund.address)
        Assertions.assertEquals("0x0000000000000000000000000000000000000000", response2.ethRefund.assetId)

        Assertions.assertEquals(
            signUserData(
                keypair2,
                hashToWithdraw(
                    "0x0000000000000000000000000000000000000000",
                    decimalAmount,
                    ethWallet,
                    hash,
                    ""
                )
            ), response2.ethSignature
        )
    }
}
