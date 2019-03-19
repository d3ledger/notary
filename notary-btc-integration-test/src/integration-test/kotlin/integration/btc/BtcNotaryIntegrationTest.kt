package integration.btc

import com.d3.commons.sidechain.iroha.CLIENT_DOMAIN
import com.d3.commons.util.getRandomString
import com.github.kittinunf.result.failure
import integration.btc.environment.BtcNotaryTestEnvironment
import integration.helper.BTC_ASSET
import integration.helper.BtcIntegrationHelperUtil
import integration.registration.RegistrationServiceTestEnvironment
import org.bitcoinj.wallet.Wallet
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.math.BigDecimal

const val DEPOSIT_WAIT_MILLIS = 10_000L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcNotaryIntegrationTest {

    private val integrationHelper = BtcIntegrationHelperUtil()
    private val environment = BtcNotaryTestEnvironment(integrationHelper)
    private val registrationServiceEnvironment = RegistrationServiceTestEnvironment(integrationHelper)


    @AfterAll
    fun dropDown() {
        registrationServiceEnvironment.close()
        environment.close()
    }

    init {
        registrationServiceEnvironment.registrationInitialization.init()
        val blockStorageFolder = File(environment.notaryConfig.bitcoin.blockStoragePath)
        //Clear bitcoin blockchain folder
        blockStorageFolder.deleteRecursively()
        //Recreate folder
        blockStorageFolder.mkdirs()
        integrationHelper.generateBtcInitialBlocks()
        integrationHelper.addNotary("test_notary", "test_notary_address")
        environment.btcNotaryInitialization.init().failure { ex -> fail("Cannot run BTC notary", ex) }
    }

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given new registered account
     * @when 1 btc was sent to new account
     * @then balance of new account is increased by 1 btc(or 100.000.000 sat), wallet file has one more UTXO
     */
    @Test
    fun testDeposit() {
        val initUTXOCount = Wallet.loadFromFile(File(environment.notaryConfig.btcTransferWalletPath)).unspents.size
        val randomName = String.getRandomString(9)
        val testClient = "$randomName@$CLIENT_DOMAIN"
        val res = registrationServiceEnvironment.register(randomName)
        assertEquals(200, res.statusCode)
        val btcAddress =
            integrationHelper.registerBtcAddress(
                environment.btcAddressGenerationConfig.btcKeysWalletPath,
                randomName,
                CLIENT_DOMAIN
            )
        val initialBalance = integrationHelper.getIrohaAccountBalance(
            testClient,
            BTC_ASSET
        )
        val btcAmount = 1
        integrationHelper.sendBtc(btcAddress, btcAmount)
        Thread.sleep(DEPOSIT_WAIT_MILLIS)
        val newBalance = integrationHelper.getIrohaAccountBalance(testClient, BTC_ASSET)
        assertEquals(
            BigDecimal(initialBalance).add(BigDecimal(btcAmount)).toString(),
            newBalance
        )
        assertTrue(environment.btcNotaryInitialization.isWatchedAddress(btcAddress))
        assertEquals(
            initUTXOCount + 1,
            Wallet.loadFromFile(File(environment.notaryConfig.btcTransferWalletPath)).unspents.size
        )
    }

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given new registered account
     * @when 1 btc was sent to new account 3 times in a row
     * @then balance of new account is increased by 3 btc(or 300.000.000 sat), wallet file has more UTXO
     */
    @Test
    fun testMultipleDeposit() {
        val initUTXOCount = Wallet.loadFromFile(File(environment.notaryConfig.btcTransferWalletPath)).unspents.size
        val totalDeposits = 3
        val randomName = String.getRandomString(9)
        val testClient = "$randomName@$CLIENT_DOMAIN"
        val res = registrationServiceEnvironment.register(randomName)
        assertEquals(200, res.statusCode)
        val btcAddress =
            integrationHelper.registerBtcAddress(
                environment.btcAddressGenerationConfig.btcKeysWalletPath,
                randomName,
                CLIENT_DOMAIN
            )
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
            BigDecimal(initialBalance).add(BigDecimal(btcAmount)).multiply(totalDeposits.toBigDecimal()).toString(),
            newBalance
        )
        assertTrue(environment.btcNotaryInitialization.isWatchedAddress(btcAddress))
        assertEquals(
            initUTXOCount + totalDeposits,
            Wallet.loadFromFile(File(environment.notaryConfig.btcTransferWalletPath)).unspents.size
        )
    }

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given new registered account
     * @when 1 btc was sent to new account 5 times in a row using multiple threads
     * @then balance of new account is increased by 3 btc(or 300.000.000 sat), wallet file has more UTXO
     */
    //TODO this test fails randomly. looks like regtest block generation issue. this must be fixed.
    @Disabled
    @Test
    fun testMultipleDepositMultiThreaded() {
        val initUTXOCount = Wallet.loadFromFile(File(environment.notaryConfig.btcTransferWalletPath)).unspents.size
        val totalDeposits = 5
        val randomName = String.getRandomString(9)
        val testClient = "$randomName@$CLIENT_DOMAIN"
        val res = registrationServiceEnvironment.register(randomName)
        assertEquals(200, res.statusCode)
        val btcAddress =
            integrationHelper.registerBtcAddress(
                environment.btcAddressGenerationConfig.btcKeysWalletPath,
                randomName,
                CLIENT_DOMAIN
            )
        val initialBalance = integrationHelper.getIrohaAccountBalance(
            testClient,
            BTC_ASSET
        )
        val btcAmount = 1
        val sendBtcThreads = ArrayList<Thread>()
        for (deposit in 1..totalDeposits) {
            val sendBtcThread = Thread {
                integrationHelper.sendBtc(btcAddress, btcAmount, 0)
            }
            sendBtcThreads.add(sendBtcThread)
            sendBtcThread.start()
        }
        sendBtcThreads.forEach { thread ->
            thread.join()
        }
        integrationHelper.generateBtcBlocks(environment.notaryConfig.bitcoin.confidenceLevel)
        Thread.sleep(15_000)
        val newBalance = integrationHelper.getIrohaAccountBalance(testClient, BTC_ASSET)
        assertEquals(
            BigDecimal(initialBalance).add(BigDecimal(btcAmount)).multiply(totalDeposits.toBigDecimal()).toString(),
            newBalance
        )
        assertTrue(environment.btcNotaryInitialization.isWatchedAddress(btcAddress))
        assertEquals(
            initUTXOCount + totalDeposits,
            Wallet.loadFromFile(File(environment.notaryConfig.btcTransferWalletPath)).unspents.size
        )
    }

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given new registered account
     * @when 1 btc was sent to new account without being properly confirmed
     * @then balance of new account stays the same, wallet file has one more UTXO
     */
    @Test
    fun testDepositNotConfirmed() {
        val initUTXOCount = Wallet.loadFromFile(File(environment.notaryConfig.btcTransferWalletPath)).unspents.size
        val randomName = String.getRandomString(9)
        val testClient = "$randomName@$CLIENT_DOMAIN"
        val res = registrationServiceEnvironment.register(randomName)
        assertEquals(200, res.statusCode)
        val btcAddress =
            integrationHelper.registerBtcAddress(
                environment.btcAddressGenerationConfig.btcKeysWalletPath,
                randomName,
                CLIENT_DOMAIN
            )
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
        assertEquals(
            initUTXOCount + 1,
            Wallet.loadFromFile(File(environment.notaryConfig.btcTransferWalletPath)).unspents.size
        )
    }

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given new registered account
     * @when 1 btc was sent to new account without being properly confirmed
     * @then balance of new account is increased by 1 btc(or 100.000.000 sat) after following confirmation
     * and wallet file has one more UTXO
     */
    @Test
    fun testDepositConfirmation() {
        val initUTXOCount = Wallet.loadFromFile(File(environment.notaryConfig.btcTransferWalletPath)).unspents.size
        val randomName = String.getRandomString(9)
        val testClient = "$randomName@$CLIENT_DOMAIN"
        val res = registrationServiceEnvironment.register(randomName)
        assertEquals(200, res.statusCode)
        val btcAddress =
            integrationHelper.registerBtcAddress(
                environment.btcAddressGenerationConfig.btcKeysWalletPath,
                randomName,
                CLIENT_DOMAIN
            )
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
            BigDecimal(initialBalance).add(BigDecimal(btcAmount)).toString(),
            newBalance
        )
        assertTrue(environment.btcNotaryInitialization.isWatchedAddress(btcAddress))
        assertEquals(
            initUTXOCount + 1,
            Wallet.loadFromFile(File(environment.notaryConfig.btcTransferWalletPath)).unspents.size
        )
    }
}
