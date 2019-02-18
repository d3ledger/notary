package integration.btc

import com.d3.btc.helper.currency.satToBtc
import com.github.kittinunf.result.failure
import integration.btc.environment.BtcWithdrawalTestEnvironment
import integration.helper.BTC_ASSET
import integration.helper.BtcIntegrationHelperUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bitcoinj.core.Address
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import sidechain.iroha.CLIENT_DOMAIN
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import com.d3.btc.withdrawal.handler.CurrentFeeRate
import java.io.File
import kotlin.test.assertEquals

private const val TOTAL_TESTS = 1

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcMultiWithdrawalIntegrationTest {

    private val peers = 3
    private val integrationHelper = BtcIntegrationHelperUtil(peers)
    private val environments = ArrayList<BtcWithdrawalTestEnvironment>()
    private lateinit var changeAddress: Address

    @AfterAll
    fun dropDown() {
        environments.forEach { environment ->
            environment.close()
        }
    }

    @BeforeAll
    fun setUp() {
        CurrentFeeRate.set(DEFAULT_FEE_RATE)
        var peerCount = 0
        integrationHelper.accountHelper.btcWithdrawalAccounts
            .forEach { withdrawalAccount ->
                integrationHelper.addNotary("test_notary_$peerCount", "test")
                val testName = "multi_withdrawal_${peerCount++}"
                val withdrawalConfig = integrationHelper.configHelper.createBtcWithdrawalConfig(testName)
                val environment =
                    BtcWithdrawalTestEnvironment(integrationHelper, testName, withdrawalConfig, withdrawalAccount)
                environments.add(environment)
                val blockStorageFolder = File(environment.btcWithdrawalConfig.bitcoin.blockStoragePath)
                //Clear bitcoin blockchain folder
                blockStorageFolder.deleteRecursively()
                //Recreate folder
                blockStorageFolder.mkdirs()
            }
        val someEnvironment = environments.first()
        integrationHelper.generateBtcInitialBlocks()
        integrationHelper.genChangeBtcAddress(someEnvironment.btcWithdrawalConfig.bitcoin.walletPath)
            .fold({ address -> changeAddress = address }, { ex -> throw  ex })
        integrationHelper.preGenFreeBtcAddresses(
            someEnvironment.btcWithdrawalConfig.bitcoin.walletPath,
            TOTAL_TESTS * 2
        )
        environments.forEach { environment ->
            environment.transactionHelper.addToBlackList(changeAddress.toBase58())
            GlobalScope.launch {
                environment.btcWithdrawalInitialization.init().failure { ex -> throw ex }
            }
        }
    }

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given 3 withdrawal services and  two registered BTC clients. 1st client has no BTC
     * @when 1st client sends SAT 10000 to 2nd client
     * @then no transaction is created, because 1st client has no money at all.
     * Money sent to 2nd client is rolled back to 1st client
     */
    @Test
    fun testWithdrawalMultisigRollback() {
        val environment = environments.first()
        val initTxCount = environment.createdTransactions.size
        val amount = satToBtc(10000L)
        val randomNameSrc = String.getRandomString(9)
        val testClientSrcKeypair = ModelUtil.generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClientSrcKeypair)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
        integrationHelper.addIrohaAssetTo(testClientSrc, BTC_ASSET, amount)
        val initialSrcBalance = integrationHelper.getIrohaAccountBalance(testClientSrc, BTC_ASSET)
        integrationHelper.transferAssetIrohaFromClient(
            testClientSrc,
            testClientSrcKeypair,
            testClientSrc,
            environment.btcWithdrawalConfig.withdrawalCredential.accountId,
            BTC_ASSET,
            btcAddressDest,
            amount.toPlainString()
        )
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS * peers)
        assertEquals(initTxCount, environment.createdTransactions.size)
        assertEquals(initialSrcBalance, integrationHelper.getIrohaAccountBalance(testClientSrc, BTC_ASSET))
        environment.transactionHelper.addToBlackList(btcAddressDest)
    }
}
