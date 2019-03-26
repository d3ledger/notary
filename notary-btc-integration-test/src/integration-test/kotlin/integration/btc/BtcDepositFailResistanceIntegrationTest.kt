package integration.btc

import com.d3.commons.sidechain.iroha.CLIENT_DOMAIN
import com.d3.commons.util.getRandomString
import com.github.kittinunf.result.failure
import integration.btc.environment.BtcNotaryTestEnvironment
import integration.helper.BTC_ASSET
import integration.helper.BtcIntegrationHelperUtil
import integration.registration.RegistrationServiceTestEnvironment
import mu.KLogging
import org.bitcoinj.core.Address
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.wallet.Wallet
import org.junit.jupiter.api.*
import java.io.File
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcDepositFailResistanceIntegrationTest {
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
    }

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given new account and transfer wallet with one unconfirmed transaction related to new account
     * @when transaction gets all the confirmations after deposit service start
     * @then balance of new account is increased by 1 btc(or 100.000.000 sat)
     * and wallet file has one more UTXO
     */
    @Test
    fun testDeposit() {
        val walletFile = File(environment.notaryConfig.btcTransferWalletPath)
        val transfersWallet = Wallet.loadFromFile(walletFile)
        val initUTXOCount = transfersWallet.unspents.size
        val randomName = String.getRandomString(9)
        val testClient = "$randomName@$CLIENT_DOMAIN"
        val res = registrationServiceEnvironment.register(randomName)
        Assertions.assertEquals(200, res.statusCode)
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
        //Simulate failure
        simulateFailRightAfterGettingDeposit(walletFile, btcAddress, btcAmount)
        //Start deposit service
        environment.btcNotaryInitialization.init().failure { ex -> fail("Cannot run BTC notary", ex) }
        //Wait a little to initiale service properly
        Thread.sleep(DEPOSIT_WAIT_MILLIS)
        // Confirm coin
        integrationHelper.generateBtcBlocks(environment.notaryConfig.bitcoin.confidenceLevel - 1)
        Thread.sleep(DEPOSIT_WAIT_MILLIS)
        val newBalance = integrationHelper.getIrohaAccountBalance(testClient, BTC_ASSET)
        Assertions.assertEquals(
            BigDecimal(initialBalance).add(BigDecimal(btcAmount)).toString(),
            newBalance
        )
        Assertions.assertTrue(environment.btcNotaryInitialization.isWatchedAddress(btcAddress))
        Assertions.assertEquals(
            initUTXOCount + 1,
            Wallet.loadFromFile(walletFile).unspents.size
        )
    }

    /**
     * Simulates failure that happens right after the first deposit transaction confirmation
     * @param walletFile - file of wallet where all the deposit transactions are stored
     * @param depositAddress - Bitcoin address to deposit
     * @param depositBtcAmount - amount of deposit
     */
    private fun simulateFailRightAfterGettingDeposit(
        walletFile: File,
        depositAddress: String,
        depositBtcAmount: Int
    ) {
        //Load wallet
        val transfersWallet = Wallet.loadFromFile(walletFile)
        val networkParams = RegTestParams.get()
        val txReceivedCountDownLatch = CountDownLatch(1)
        //Watch address
        transfersWallet.addWatchedAddress(Address.fromBase58(networkParams, depositAddress))
        //Listen for transaction
        transfersWallet.addCoinsReceivedEventListener { _, tx, _, _ ->
            logger.info { "Got coin ${tx.hashAsString}" }
            //Save wallet after getting a coin
            transfersWallet.saveToFile(walletFile)
            txReceivedCountDownLatch.countDown()
        }
        //Creates peer group
        val peerGroup = environment.createPeerGroup(transfersWallet)
        peerGroup.startAsync()
        peerGroup.downloadBlockChain()
        //Send coins and confirm it with exactly one block
        integrationHelper.sendBtc(
            depositAddress,
            depositBtcAmount,
            1
        )
        //Wait until we get a coin
        txReceivedCountDownLatch.await(10, TimeUnit.SECONDS)
        //Stop peer group
        peerGroup.stop()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
