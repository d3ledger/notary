package integration.btc

import com.d3.btc.helper.address.outPutToBase58Address
import com.d3.btc.helper.currency.satToBtc
import com.d3.btc.model.BtcAddressType
import com.d3.btc.provider.generation.ADDRESS_GENERATION_NODE_ID_KEY
import com.d3.btc.provider.generation.ADDRESS_GENERATION_TIME_KEY
import com.d3.btc.withdrawal.handler.CurrentFeeRate
import com.d3.commons.sidechain.iroha.CLIENT_DOMAIN
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import com.github.kittinunf.result.failure
import integration.btc.environment.BtcAddressGenerationTestEnvironment
import integration.btc.environment.BtcWithdrawalTestEnvironment
import integration.helper.BTC_ASSET
import integration.helper.BtcIntegrationHelperUtil
import integration.registration.RegistrationServiceTestEnvironment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bitcoinj.params.RegTestParams
import org.junit.jupiter.api.*
import java.io.File
import java.math.BigDecimal
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcMultiWithdrawalIntegrationTest {

    private val peers = 3
    private val integrationHelper = BtcIntegrationHelperUtil(peers)
    private val withdrawalEnvironments = ArrayList<BtcWithdrawalTestEnvironment>()
    private val addressGenerationEnvironments = ArrayList<BtcAddressGenerationTestEnvironment>()
    private val registrationServiceEnvironment = RegistrationServiceTestEnvironment(integrationHelper)


    @AfterAll
    fun dropDown() {
        registrationServiceEnvironment.close()

        withdrawalEnvironments.forEach { environment ->
            environment.close()
        }
        addressGenerationEnvironments.forEach { environment ->
            environment.close()
        }
    }

    @BeforeAll
    fun setUp() {
        registrationServiceEnvironment.registrationInitialization.init()
        integrationHelper.generateBtcInitialBlocks()
        CurrentFeeRate.set(DEFAULT_FEE_RATE)
        val testNames = ArrayList<String>()
        repeat(peers) { peer ->
            testNames.add("multi_withdrawal_${String.getRandomString(5)}_$peer")
            integrationHelper.addNotary("test_notary_$peer", "test")
        }
        var peerCount = 0
        integrationHelper.accountHelper.btcWithdrawalAccounts
            .forEach { withdrawalAccount ->
                val testName = testNames[peerCount++]
                val withdrawalConfig = integrationHelper.configHelper.createBtcWithdrawalConfig(testName)
                val environment =
                    BtcWithdrawalTestEnvironment(integrationHelper, testName, withdrawalConfig, withdrawalAccount)
                environment.withdrawalTransferService.addNewBtcTransactionListener { tx ->
                    environment.createdTransactions[tx.hashAsString] = Pair(System.currentTimeMillis(), tx)
                }
                withdrawalEnvironments.add(environment)
                val blockStorageFolder = File(environment.btcWithdrawalConfig.bitcoin.blockStoragePath)
                //Clear bitcoin blockchain folder
                blockStorageFolder.deleteRecursively()
                //Recreate folder
                blockStorageFolder.mkdirs()
            }

        peerCount = 0
        integrationHelper.accountHelper.mstRegistrationAccounts.forEach { mstRegistrationAccount ->
            val testName = testNames[peerCount++]
            val environment = BtcAddressGenerationTestEnvironment(
                integrationHelper,
                btcGenerationConfig = integrationHelper.configHelper.createBtcAddressGenerationConfig(0, testName),
                mstRegistrationCredential = mstRegistrationAccount
            )
            addressGenerationEnvironments.add(environment)
            GlobalScope.launch {
                environment.btcAddressGenerationInitialization.init().failure { ex -> throw ex }
            }
        }
        //Wait services to init
        Thread.sleep(WAIT_PREGEN_PROCESS_MILLIS)
        generateAddress(BtcAddressType.CHANGE)

        withdrawalEnvironments.forEach { environment ->
            GlobalScope.launch {
                environment.btcWithdrawalInitialization.init().failure { ex -> throw ex }
            }
        }
    }

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given 3 withdrawal services and  two registered BTC clients. 1st client has no BTC
     * @when 1st client sends SAT 1 to 2nd client
     * @then no transaction is created, because SAT 1 is considered dust.
     * Money sent to 2nd client is rolled back to 1st client
     */
    @Test
    fun testWithdrawalMultisigRollback() {
        val environment = withdrawalEnvironments.first()
        val initTxCount = environment.createdTransactions.size
        val amount = satToBtc(1L)
        val randomNameSrc = String.getRandomString(9)
        val testClientSrcKeypair = ModelUtil.generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        registrationServiceEnvironment.register(randomNameSrc, testClientSrcKeypair.public.toHexString())
        generateAddress(BtcAddressType.FREE)
        generateAddress(BtcAddressType.FREE)
        integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, CLIENT_DOMAIN, testClientSrcKeypair)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest, CLIENT_DOMAIN)
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

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given two registered BTC clients. 1st client has 1 BTC in wallet.
     * @when 1st client sends SAT 10000 to 2nd client
     * @then new well constructed BTC transaction and one signature appear.
     * Transaction is properly signed, sent to Bitcoin network and not considered unsigned anymore
     */
    @Test
    fun testWithdrawal() {
        val environment = withdrawalEnvironments.first()
        val initTxCount = environment.createdTransactions.size
        val amount = satToBtc(10000L)
        val randomNameSrc = String.getRandomString(9)
        val testClientSrcKeypair = ModelUtil.generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        registrationServiceEnvironment.register(randomNameSrc, testClientSrcKeypair.public.toHexString())
        generateAddress(BtcAddressType.FREE)
        generateAddress(BtcAddressType.FREE)
        val btcAddressSrc =
            integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, CLIENT_DOMAIN, testClientSrcKeypair)
        integrationHelper.sendBtc(btcAddressSrc, 1, environment.btcWithdrawalConfig.bitcoin.confidenceLevel)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest, CLIENT_DOMAIN)
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
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount + 1, environment.createdTransactions.size)
        val createdWithdrawalTx = environment.getLastCreatedTxHash()
        environment.signCollector.getSignatures(createdWithdrawalTx).fold({ signatures ->
            BtcWithdrawalIntegrationTest.logger.info { "signatures $signatures" }
            assertEquals(peers, signatures[0]!!.size)
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
        Assertions.assertNotNull(environment.getLastCreatedTx().outputs.firstOrNull { transactionOutput ->
            outPutToBase58Address(
                transactionOutput
            ) == btcAddressDest
        })
    }

    /**
     * Generates address
     * @param addressType - type of address to generate
     * @return address
     */
    private fun generateAddress(addressType: BtcAddressType): String {
        val environment = addressGenerationEnvironments.first()
        val sessionAccountName = addressType.createSessionAccountName()
        environment.btcKeyGenSessionProvider.createPubKeyCreationSession(
            sessionAccountName,
            environment.btcGenerationConfig.nodeId
        ).fold({ BtcAddressGenerationIntegrationTest.logger.info { "session $sessionAccountName was created" } },
            { ex -> fail("cannot create session", ex) })
        environment.triggerProvider.trigger(sessionAccountName)
        Thread.sleep(WAIT_PREGEN_PROCESS_MILLIS)
        val sessionDetails =
            integrationHelper.getAccountDetails(
                "$sessionAccountName@btcSession",
                environment.btcGenerationConfig.registrationAccount.accountId
            )
        val notaryKeys =
            sessionDetails.entries.filter { entry ->
                entry.key != ADDRESS_GENERATION_TIME_KEY
                        && entry.key != ADDRESS_GENERATION_NODE_ID_KEY
            }.map { entry -> entry.value }
        return com.d3.btc.helper.address.createMsAddress(notaryKeys, RegTestParams.get()).toBase58()
    }
}
