package integration.btc

import com.github.kittinunf.result.failure
import integration.btc.environment.BtcNotaryTestEnvironment
import integration.helper.BTC_ASSET
import integration.helper.BtcIntegrationHelperUtil
import org.bitcoinj.core.Address
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import sidechain.iroha.CLIENT_DOMAIN
import util.getRandomString
import java.io.File
import java.math.BigDecimal

const val DEPOSIT_WAIT_MILLIS = 20_000L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcNotaryIntegrationTest {

    private val integrationHelper = BtcIntegrationHelperUtil()
    private val environment = BtcNotaryTestEnvironment(integrationHelper)

    @AfterAll
    fun dropDown() {
        environment.close()
    }

    init {
        val blockStorageFolder = File(environment.notaryConfig.bitcoin.blockStoragePath)
        //Clear bitcoin blockchain folder
        blockStorageFolder.deleteRecursively()
        //Recreate folder
        blockStorageFolder.mkdirs()
        integrationHelper.generateBtcBlocks()
        integrationHelper.addNotary("test_notary", "test_notary_address")
        environment.btcNotaryInitialization.init().failure { ex -> fail("Cannot run BTC notary", ex) }
    }

    /**
     * Test US-001 Deposit
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given new registered account
     * @when 1 btc was sent to new account
     * @then balance of new account is increased by 1 btc(or 100.000.000 sat)
     */
    @Test
    fun testDeposit() {
        val randomName = String.getRandomString(9)
        val testClient = "$randomName@$CLIENT_DOMAIN"
        val btcAddress =
            integrationHelper.registerBtcAddress(environment.notaryConfig.bitcoin.walletPath, randomName)
        val initialBalance = integrationHelper.getIrohaAccountBalance(
            testClient,
            BTC_ASSET
        )
        val btcAmount = 1
        integrationHelper.sendBtc(btcAddress, btcAmount)
        Thread.sleep(DEPOSIT_WAIT_MILLIS)
        val newBalance = integrationHelper.getIrohaAccountBalance(testClient, BTC_ASSET)
        assertEquals(
            BigDecimal(initialBalance).add(integrationHelper.btcToSat(btcAmount)).toString(),
            newBalance
        )
        assertTrue(environment.btcNotaryInitialization.isWatchedAddress(btcAddress))
    }

    /**
     * Test US-002 Deposit
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given new registered account
     * @when 1 btc was sent to new account 3 times in a row
     * @then balance of new account is increased by 3 btc(or 300.000.000 sat)
     */
    @Test
    fun testMultipleDeposit() {
        val totalDeposits = 3
        val randomName = String.getRandomString(9)
        val testClient = "$randomName@$CLIENT_DOMAIN"
        val btcAddress =
            integrationHelper.registerBtcAddress(environment.notaryConfig.bitcoin.walletPath, randomName)
        val initialBalance = integrationHelper.getIrohaAccountBalance(
            testClient,
            BTC_ASSET
        )
        val btcAmount = 1
        for (deposit in 1..totalDeposits) {
            integrationHelper.sendBtc(btcAddress, btcAmount)
            Thread.sleep(DEPOSIT_WAIT_MILLIS)
        }
        val newBalance = integrationHelper.getIrohaAccountBalance(testClient, BTC_ASSET)
        assertEquals(
            BigDecimal(initialBalance).add(integrationHelper.btcToSat(btcAmount)).multiply(totalDeposits.toBigDecimal()).toString(),
            newBalance
        )
        assertTrue(environment.btcNotaryInitialization.isWatchedAddress(btcAddress))
    }

    /**
     * Test US-003 Deposit
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given new registered account
     * @when 1 btc was sent to new account without being properly confirmed
     * @then balance of new account stays the same
     */
    @Test
    fun testDepositNotConfirmed() {
        val randomName = String.getRandomString(9)
        val testClient = "$randomName@$CLIENT_DOMAIN"
        val btcAddress =
            integrationHelper.registerBtcAddress(environment.notaryConfig.bitcoin.walletPath, randomName)
        val initialBalance = integrationHelper.getIrohaAccountBalance(
            testClient,
            BTC_ASSET
        )
        val btcAmount = 1
        integrationHelper.sendBtc(
            btcAddress,
            btcAmount,
            environment.notaryConfig.bitcoin.confidenceLevel - 1
        )
        Thread.sleep(DEPOSIT_WAIT_MILLIS)
        val newBalance = integrationHelper.getIrohaAccountBalance(testClient, BTC_ASSET)
        assertEquals(initialBalance, newBalance)
        assertTrue(environment.btcNotaryInitialization.isWatchedAddress(btcAddress))
    }

    /**
     * Test US-004 Deposit
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given new registered account
     * @when 1 btc was sent to new account without being properly confirmed
     * @then balance of new account is increased by 1 btc(or 100.000.000 sat) after following confirmation
     */
    @Test
    fun testDepositConfirmation() {
        val randomName = String.getRandomString(9)
        val testClient = "$randomName@$CLIENT_DOMAIN"
        val btcAddress =
            integrationHelper.registerBtcAddress(environment.notaryConfig.bitcoin.walletPath, randomName)
        val initialBalance = integrationHelper.getIrohaAccountBalance(
            testClient,
            BTC_ASSET
        )
        val btcAmount = 1
        integrationHelper.sendBtc(btcAddress, btcAmount, 0)
        Thread.sleep(DEPOSIT_WAIT_MILLIS)
        for (confirmation in 1..environment.notaryConfig.bitcoin.confidenceLevel) {
            Thread.sleep(150)
            integrationHelper.generateBtcBlocks(1)
        }
        Thread.sleep(DEPOSIT_WAIT_MILLIS)
        val newBalance = integrationHelper.getIrohaAccountBalance(testClient, BTC_ASSET)
        assertEquals(
            BigDecimal(initialBalance).add(integrationHelper.btcToSat(btcAmount)).toString(),
            newBalance
        )
        assertTrue(environment.btcNotaryInitialization.isWatchedAddress(btcAddress))
    }

    //Checks if address is in set of watched address
    private fun addressIsWatched(btcAddress: String, watchedAddresses: List<Address>): Boolean {
        return watchedAddresses.find { address -> address.toBase58() == btcAddress } != null
    }
}
