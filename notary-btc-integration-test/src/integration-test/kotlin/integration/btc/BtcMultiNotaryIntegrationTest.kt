package integration.btc

import com.github.kittinunf.result.failure
import integration.btc.environment.BtcNotaryTestEnvironment
import integration.helper.BTC_ASSET
import integration.helper.BtcIntegrationHelperUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import sidechain.iroha.CLIENT_DOMAIN
import util.getRandomString
import java.io.File
import java.math.BigDecimal


@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcMultiNotaryIntegrationTest {

    private val peers = 3
    private val integrationHelper = BtcIntegrationHelperUtil(peers)
    private val environments = ArrayList<BtcNotaryTestEnvironment>()

    init {
        var peerCount = 0
        //Create configs for multiple notary services
        integrationHelper.accountHelper.notaryAccounts
            .forEach { notaryAccount ->
                val testName = "multi_notary_${peerCount++}"
                val notaryConfig = integrationHelper.configHelper.createBtcDepositConfig(testName, notaryAccount)
                environments.add(
                    BtcNotaryTestEnvironment(
                        integrationHelper = integrationHelper,
                        notaryConfig = notaryConfig,
                        notaryCredential = notaryAccount,
                        testName = testName
                    )
                )
                val blockStorageFolder = File(notaryConfig.bitcoin.blockStoragePath)
                //Clear bitcoin blockchain folder
                blockStorageFolder.deleteRecursively()
                //Recreate folder
                blockStorageFolder.mkdirs()
                integrationHelper.addNotary(testName, "test")

            }
        integrationHelper.generateBtcInitialBlocks()
        environments.forEach { environment ->
            GlobalScope.launch {
                environment.btcNotaryInitialization.init()
                    .failure { ex -> fail("Cannot run BTC notary", ex) }
            }
        }
    }

    @AfterAll
    fun dropDown() {
        environments.forEach { environment ->
            environment.close()
        }
    }

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given 3 notary services working in a multisig mode and new registered account
     * @when 1 btc was sent to new account
     * @then balance of new account is increased by 1 btc(or 100.000.000 sat)
     */
    @Test
    fun testDeposit() {
        val randomName = String.getRandomString(9)
        val testClient = "$randomName@$CLIENT_DOMAIN"
        val btcAddress =
            integrationHelper.registerBtcAddress(
                environments.first().notaryConfig.btcTransferWalletPath,
                randomName
            )
        val initialBalance = integrationHelper.getIrohaAccountBalance(
            testClient,
            BTC_ASSET
        )
        val btcAmount = 1
        integrationHelper.sendBtc(btcAddress, btcAmount)
        Thread.sleep(DEPOSIT_WAIT_MILLIS * peers)
        val newBalance = integrationHelper.getIrohaAccountBalance(testClient, BTC_ASSET)
        assertEquals(
            BigDecimal(initialBalance).add(BigDecimal(btcAmount)).toString(),
            newBalance
        )
    }
}
