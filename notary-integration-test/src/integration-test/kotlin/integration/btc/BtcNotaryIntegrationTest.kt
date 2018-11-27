package integration.btc

import com.github.kittinunf.result.failure
import handler.btc.NewBtcClientRegistrationHandler
import integration.helper.ConfigHelper
import integration.helper.IntegrationHelperUtil
import integration.helper.btcAsset
import listener.btc.NewBtcClientRegistrationListener
import model.IrohaCredential
import notary.btc.BtcNotaryInitialization
import org.bitcoinj.core.Address
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import provider.btc.address.BtcRegisteredAddressesProvider
import provider.btc.network.BtcRegTestConfigProvider
import sidechain.iroha.CLIENT_DOMAIN
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import java.io.File
import java.math.BigDecimal
import java.time.Duration

private val integrationHelper = IntegrationHelperUtil()

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcNotaryIntegrationTest {

    private val notaryConfig = integrationHelper.configHelper.createBtcNotaryConfig()
    private val irohaCredential = IrohaCredential(
        notaryConfig.notaryCredential.accountId,
        ModelUtil.loadKeypair(notaryConfig.notaryCredential.pubkeyPath, notaryConfig.notaryCredential.privkeyPath).get()
    )

    private val irohaChainListener = IrohaChainListener(
        notaryConfig.iroha.hostname,
        notaryConfig.iroha.port,
        irohaCredential
    )

    private val btcRegisteredAddressesProvider = BtcRegisteredAddressesProvider(
        irohaCredential,
        integrationHelper.irohaNetwork,
        notaryConfig.registrationAccount,
        notaryConfig.notaryCredential.accountId
    )

    private val btcNetworkConfigProvider = BtcRegTestConfigProvider()

    private val newBtcClientRegistrationListener =
        NewBtcClientRegistrationListener(NewBtcClientRegistrationHandler(btcNetworkConfigProvider))

    private val btcNotaryInitialization =
        BtcNotaryInitialization(
            notaryConfig,
            irohaCredential,
            integrationHelper.irohaNetwork,
            btcRegisteredAddressesProvider,
            irohaChainListener,
            newBtcClientRegistrationListener,
            btcNetworkConfigProvider
        )

    private val timeoutDuration = Duration.ofMinutes(ConfigHelper.timeoutMinutes)

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
        irohaChainListener.close()
        //Clear bitcoin blockchain folder
        File(notaryConfig.bitcoin.blockStoragePath).deleteRecursively()
    }

    init {
        val blockStorageFolder = File(notaryConfig.bitcoin.blockStoragePath)
        //Clear bitcoin blockchain folder
        blockStorageFolder.deleteRecursively()
        //Recreate folder
        blockStorageFolder.mkdirs()
        integrationHelper.generateBtcBlocks()
        integrationHelper.addNotary("test_notary", "test_notary_address")
        btcNotaryInitialization.init().failure { ex -> fail("Cannot run BTC notary", ex) }
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
        assertTimeoutPreemptively(timeoutDuration) {
            val randomName = String.getRandomString(9)
            val testClient = "$randomName@$CLIENT_DOMAIN"
            val btcAddress = integrationHelper.registerBtcAddress(notaryConfig.bitcoin.walletPath, randomName)
            val initialBalance = integrationHelper.getIrohaAccountBalance(
                testClient,
                btcAsset
            )
            val btcAmount = 1
            integrationHelper.sendBtc(btcAddress, btcAmount)
            Thread.sleep(20_000)
            val newBalance = integrationHelper.getIrohaAccountBalance(testClient, btcAsset)
            assertEquals(
                BigDecimal(initialBalance).add(integrationHelper.btcToSat(btcAmount).toBigDecimal()).toString(),
                newBalance
            )
            assertTrue(btcNotaryInitialization.isWatchedAddress(btcAddress))
        }
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
        assertTimeoutPreemptively(timeoutDuration) {
            val totalDeposits = 3
            val randomName = String.getRandomString(9)
            val testClient = "$randomName@$CLIENT_DOMAIN"
            val btcAddress = integrationHelper.registerBtcAddress(notaryConfig.bitcoin.walletPath, randomName)
            val initialBalance = integrationHelper.getIrohaAccountBalance(
                testClient,
                btcAsset
            )
            val btcAmount = 1
            for (deposit in 1..totalDeposits) {
                integrationHelper.sendBtc(btcAddress, btcAmount)
                Thread.sleep(20_000)
            }
            val newBalance = integrationHelper.getIrohaAccountBalance(testClient, btcAsset)
            assertEquals(
                BigDecimal(initialBalance).add(integrationHelper.btcToSat(btcAmount).toBigDecimal()).multiply(
                    totalDeposits.toBigDecimal()
                ).toString(),
                newBalance
            )
            assertTrue(btcNotaryInitialization.isWatchedAddress(btcAddress))
        }
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
        assertTimeoutPreemptively(timeoutDuration) {
            val randomName = String.getRandomString(9)
            val testClient = "$randomName@$CLIENT_DOMAIN"
            val btcAddress = integrationHelper.registerBtcAddress(notaryConfig.bitcoin.walletPath, randomName)
            val initialBalance = integrationHelper.getIrohaAccountBalance(
                testClient,
                btcAsset
            )
            val btcAmount = 1
            integrationHelper.sendBtc(btcAddress, btcAmount, notaryConfig.bitcoin.confidenceLevel - 1)
            Thread.sleep(20_000)
            val newBalance = integrationHelper.getIrohaAccountBalance(testClient, btcAsset)
            assertEquals(initialBalance, newBalance)
            assertTrue(btcNotaryInitialization.isWatchedAddress(btcAddress))
        }
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
        assertTimeoutPreemptively(timeoutDuration) {
            val randomName = String.getRandomString(9)
            val testClient = "$randomName@$CLIENT_DOMAIN"
            val btcAddress = integrationHelper.registerBtcAddress(notaryConfig.bitcoin.walletPath, randomName)
            val initialBalance = integrationHelper.getIrohaAccountBalance(
                testClient,
                btcAsset
            )
            val btcAmount = 1
            integrationHelper.sendBtc(btcAddress, btcAmount, 0)
            Thread.sleep(20_000)
            for (confirmation in 1..notaryConfig.bitcoin.confidenceLevel) {
                Thread.sleep(150)
                integrationHelper.generateBtcBlocks(1)
            }
            Thread.sleep(20_000)
            val newBalance = integrationHelper.getIrohaAccountBalance(testClient, btcAsset)
            assertEquals(
                BigDecimal(initialBalance).add(integrationHelper.btcToSat(btcAmount).toBigDecimal()).toString(),
                newBalance
            )
            assertTrue(btcNotaryInitialization.isWatchedAddress(btcAddress))
        }
    }

    //Checks if address is in set of watched address
    private fun addressIsWatched(btcAddress: String, watchedAddresses: List<Address>): Boolean {
        return watchedAddresses.find { address -> address.toBase58() == btcAddress } != null
    }
}
