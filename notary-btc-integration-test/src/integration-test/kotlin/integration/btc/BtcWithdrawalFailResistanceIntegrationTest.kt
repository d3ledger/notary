package integration.btc

import com.d3.btc.helper.address.outPutToBase58Address
import com.d3.btc.helper.currency.satToBtc
import com.d3.btc.withdrawal.handler.CurrentFeeRate
import com.github.kittinunf.result.failure
import integration.btc.environment.BtcWithdrawalTestEnvironment
import integration.helper.BTC_ASSET
import integration.helper.BtcIntegrationHelperUtil
import org.bitcoinj.core.Address
import org.junit.Assert.assertNotNull
import org.junit.jupiter.api.*
import sidechain.iroha.CLIENT_DOMAIN
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import java.io.File
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private const val TOTAL_TESTS = 1

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcWithdrawalFailResistanceIntegrationTest {

    private val integrationHelper = BtcIntegrationHelperUtil()

    private val environment =
        BtcWithdrawalTestEnvironment(integrationHelper, "fail_resistance_${String.getRandomString(5)}")

    private lateinit var changeAddress: Address

    @AfterAll
    fun dropDown() {
        environment.close()
    }

    @BeforeAll
    fun setUp() {
        // This call simulates that the service stopped
        environment.bindQueueWithExchange(
            environment.btcWithdrawalConfig.irohaBlockQueue,
            environment.rmqConfig.irohaExchange
        )
        CurrentFeeRate.set(DEFAULT_FEE_RATE)
        val blockStorageFolder = File(environment.btcWithdrawalConfig.bitcoin.blockStoragePath)
        //Clear bitcoin blockchain folder
        blockStorageFolder.deleteRecursively()
        //Recreate folder
        blockStorageFolder.mkdirs()
        integrationHelper.addNotary("test", "test")
        integrationHelper.generateBtcInitialBlocks()
        integrationHelper.genChangeBtcAddress(environment.btcWithdrawalConfig.btcKeysWalletPath)
            .fold({ address -> changeAddress = address }, { ex -> throw  ex })
        integrationHelper.preGenFreeBtcAddresses(environment.btcWithdrawalConfig.btcKeysWalletPath, TOTAL_TESTS * 2)
        environment.withdrawalTransferEventHandler.addNewBtcTransactionListener { tx ->
            environment.createdTransactions[tx.hashAsString] = Pair(System.currentTimeMillis(), tx)
        }
        environment.transactionHelper.addToBlackList(changeAddress.toBase58())
    }

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given two registered BTC clients. 1st client has 1 BTC in wallet. btc-withdrawal service is off
     * @when 1st client sends SAT 10000 to 2nd client, btc-withdrawal service is turned on
     * @then new well constructed BTC transaction and one signature appear.
     * Transaction is properly signed, sent to Bitcoin network and not considered unsigned anymore
     */
    @Test
    fun testWithdrawalBeforeServiceStart() {
        val initTxCount = environment.createdTransactions.size
        val amount = satToBtc(10000L)
        val randomNameSrc = String.getRandomString(9)
        val testClientSrcKeypair = ModelUtil.generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClientSrcKeypair)
        integrationHelper.sendBtc(btcAddressSrc, 1, environment.btcWithdrawalConfig.bitcoin.confidenceLevel)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
        integrationHelper.addIrohaAssetTo(testClientSrc, BTC_ASSET, amount)
        integrationHelper.transferAssetIrohaFromClient(
            testClientSrc,
            testClientSrcKeypair,
            testClientSrc,
            environment.btcWithdrawalConfig.withdrawalCredential.accountId,
            BTC_ASSET,
            btcAddressDest,
            amount.toPlainString()
        )

        // Start withdrawal service after transfer
        environment.btcWithdrawalInitialization.init().failure { ex -> throw ex }

        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount + 1, environment.createdTransactions.size)
        val createdWithdrawalTx = environment.getLastCreatedTxHash()
        environment.signCollector.getSignatures(createdWithdrawalTx).fold({ signatures ->
            BtcWithdrawalIntegrationTest.logger.info { "signatures $signatures" }
            assertEquals(1, signatures[0]!!.size)
        }, { ex -> fail(ex) })
        environment.transactionHelper.addToBlackList(btcAddressSrc)
        environment.transactionHelper.addToBlackList(btcAddressDest)
        assertEquals(
            0,
            BigDecimal.ZERO.compareTo(
                BigDecimal(integrationHelper.getIrohaAccountBalance(testClientSrc, BTC_ASSET))
            )
        )
        assertFalse(environment.unsignedTransactions.isUnsigned(createdWithdrawalTx))
        assertEquals(2, environment.getLastCreatedTx().outputs.size)
        assertNotNull(environment.getLastCreatedTx().outputs.firstOrNull { transactionOutput ->
            outPutToBase58Address(
                transactionOutput
            ) == btcAddressDest
        })
        assertNotNull(environment.getLastCreatedTx().outputs.firstOrNull { transactionOutput ->
            outPutToBase58Address(
                transactionOutput
            ) == changeAddress.toBase58()
        })
    }
}
