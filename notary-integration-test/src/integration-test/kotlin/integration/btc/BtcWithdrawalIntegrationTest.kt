package integration.btc

import integration.helper.IntegrationHelperUtil
import integration.helper.btcAsset
import jp.co.soramitsu.iroha.ModelCrypto
import model.IrohaCredential
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import sidechain.iroha.CLIENT_DOMAIN
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import withdrawal.btc.BtcWithdrawalInitialization

//TODO this is just a framework for btc withdrawal tests. new code will be added in the near future.
@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcWithdrawalIntegrationTest {
    private val integrationHelper = IntegrationHelperUtil()

    private val btcWithdrawalConfig = integrationHelper.configHelper.createBtcWithdrawalConfig()

    private val withdrawalKeypair = ModelUtil.loadKeypair(
        btcWithdrawalConfig.withdrawalCredential.pubkeyPath,
        btcWithdrawalConfig.withdrawalCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    private val withdrawalCredential =
        IrohaCredential(btcWithdrawalConfig.withdrawalCredential.accountId, withdrawalKeypair)

    private val irohaChainListener = IrohaChainListener(
        btcWithdrawalConfig.iroha.hostname,
        btcWithdrawalConfig.iroha.port,
        withdrawalCredential
    )
    private val btcWithdrawalInitialization = BtcWithdrawalInitialization(btcWithdrawalConfig, irohaChainListener)

    init {
        btcWithdrawalInitialization.init()
    }

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
        irohaChainListener.close()
    }

    @Test
    fun testWithdrawal() {
        val amount = "10000"
        val randomNameSrc = String.getRandomString(9)
        val testClientDestKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        integrationHelper.registerBtcAddress(randomNameSrc, testClientDestKeypair)

        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddress(randomNameDest)

        integrationHelper.addIrohaAssetTo(testClientSrc, btcAsset, amount)

        integrationHelper.transferAssetIrohaFromClient(
            testClientSrc,
            testClientDestKeypair,
            testClientSrc,
            btcWithdrawalConfig.withdrawalCredential.accountId,
            btcAsset,
            btcAddressDest,
            amount
        )
    }
}
