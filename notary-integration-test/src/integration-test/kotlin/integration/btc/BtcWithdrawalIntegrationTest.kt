package integration.btc

import com.github.kittinunf.result.failure
import helper.address.outPutToBase58Address
import integration.helper.IntegrationHelperUtil
import integration.helper.btcAsset
import jp.co.soramitsu.iroha.ModelCrypto
import model.IrohaCredential
import mu.KLogging
import org.bitcoinj.core.Address
import org.bitcoinj.core.TransactionOutput
import org.junit.jupiter.api.*
import provider.btc.address.BtcRegisteredAddressesProvider
import provider.btc.network.BtcNetworkConfigProvider
import provider.btc.network.BtcRegTestConfigProvider
import sidechain.iroha.CLIENT_DOMAIN
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import withdrawal.btc.BtcWithdrawalInitialization
import withdrawal.btc.handler.NewSignatureEventHandler
import withdrawal.btc.handler.WithdrawalTransferEventHandler
import withdrawal.btc.provider.BtcWhiteListProvider
import withdrawal.btc.transaction.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private const val WITHDRAWAL_WAIT_MILLIS = 15_000L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcWithdrawalIntegrationTest {
    private val integrationHelper = IntegrationHelperUtil()

    private val createdTransactions = ConcurrentHashMap<String, TimedTx>()

    private val btcWithdrawalConfig = integrationHelper.configHelper.createBtcWithdrawalConfig()

    init {
        File(btcWithdrawalConfig.bitcoin.blockStoragePath).mkdirs()
    }

    private val withdrawalKeypair = ModelUtil.loadKeypair(
        btcWithdrawalConfig.withdrawalCredential.pubkeyPath,
        btcWithdrawalConfig.withdrawalCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    private val withdrawalCredential =
        IrohaCredential(btcWithdrawalConfig.withdrawalCredential.accountId, withdrawalKeypair)

    private val irohaNetwork = IrohaNetworkImpl(btcWithdrawalConfig.iroha.hostname, btcWithdrawalConfig.iroha.port)

    private val withdrawalIrohaConsumer = IrohaConsumerImpl(
        withdrawalCredential,
        irohaNetwork
    )

    private val irohaChainListener = IrohaChainListener(
        btcWithdrawalConfig.iroha.hostname,
        btcWithdrawalConfig.iroha.port,
        withdrawalCredential
    )

    private val btcRegisteredAddressesProvider = BtcRegisteredAddressesProvider(
        integrationHelper.testCredential,
        integrationHelper.irohaNetwork,
        btcWithdrawalConfig.registrationAccount,
        btcWithdrawalConfig.notaryCredential.accountId
    )

    private val btcNetworkConfigProvider = BtcRegTestConfigProvider()
    private val changeAddress =
        Address.fromBase58(btcNetworkConfigProvider.getConfig(), btcWithdrawalConfig.changeAddress)
    private val transactionHelper =
        BlackListableTransactionHelper(btcNetworkConfigProvider, btcRegisteredAddressesProvider)
    private val transactionCreator =
        TransactionCreator(changeAddress, btcNetworkConfigProvider, transactionHelper)
    private val transactionSigner = TransactionSigner(btcRegisteredAddressesProvider)
    private val signCollector =
        SignCollector(
            irohaNetwork,
            withdrawalCredential,
            withdrawalIrohaConsumer,
            transactionSigner
        )

    private val unsignedTransactions = UnsignedTransactions(signCollector)
    private val withdrawalTransferEventHandler = WithdrawalTransferEventHandler(
        BtcWhiteListProvider(
            btcWithdrawalConfig.registrationAccount, withdrawalCredential, irohaNetwork
        ), btcWithdrawalConfig, transactionCreator, signCollector, unsignedTransactions
    )
    private val newSignatureEventHandler = NewSignatureEventHandler(signCollector, unsignedTransactions)

    private val btcWithdrawalInitialization =
        BtcWithdrawalInitialization(
            btcWithdrawalConfig,
            irohaChainListener,
            btcNetworkConfigProvider,
            withdrawalTransferEventHandler,
            newSignatureEventHandler
        )

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
        irohaChainListener.close()
        File(btcWithdrawalConfig.bitcoin.blockStoragePath).deleteRecursively()
    }

    @BeforeAll
    fun setUp() {
        val blockStorageFolder = File(btcWithdrawalConfig.bitcoin.blockStoragePath)
        //Clear bitcoin blockchain folder
        blockStorageFolder.deleteRecursively()
        //Recreate folder
        blockStorageFolder.mkdirs()
        integrationHelper.generateBtcBlocks()
        //TODO make it faster
        integrationHelper.preGenBtcAddresses(btcWithdrawalConfig.bitcoin.walletPath, 18).failure { ex -> throw ex }
        btcWithdrawalInitialization.init()
        withdrawalTransferEventHandler.addNewBtcTransactionListener { tx ->
            createdTransactions[tx.hashAsString] = TimedTx.create(tx)
        }
    }

    /**
     * Test US-001 Withdrawal
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given two registered BTC clients. 1st client has 1 BTC in wallet.
     * @when 1st client sends SAT 1000 to 2nd client
     * @then new well constructed BTC transaction and one signature appear.
     * Transaction is properly signed, sent to Bitcoin network and not considered unsigned anymore
     */
    @Test
    fun testWithdrawal() {
        val initTxCount = createdTransactions.size
        val amount = "10000"
        val randomNameSrc = String.getRandomString(9)
        val testClientDestKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClientDestKeypair)
        integrationHelper.sendBtc(btcAddressSrc, 1, btcWithdrawalConfig.bitcoin.confidenceLevel)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
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
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount + 1, createdTransactions.size)
        val createdWithdrawalTx = getLastCreatedTx(createdTransactions)
        signCollector.getSignatures(createdWithdrawalTx).fold({ signatures ->
            logger.info { "signatures $signatures" }
            assertEquals(1, signatures[0]!!.size)
        }, { ex -> fail(ex) })
        transactionHelper.addToBlackList(btcAddressSrc)
        transactionHelper.addToBlackList(btcAddressDest)
        assertFalse(unsignedTransactions.isUnsigned(createdWithdrawalTx))
    }

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given two registered BTC clients. 1st client has 2 BTC as 2 unspents(1+1) in wallet
     * @when 1st client sends 2 BTC to 2nd client
     * @then no tx is created, because 1st client has no money for fee
     */
    @Test
    fun testWithdrawalNoMoneyLeftForFee() {
        val initTxCount = createdTransactions.size
        val amount = integrationHelper.btcToSat(2)
        val randomNameSrc = String.getRandomString(9)
        val testClientDestKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClientDestKeypair)
        integrationHelper.sendBtc(btcAddressSrc, 1, btcWithdrawalConfig.bitcoin.confidenceLevel)
        integrationHelper.sendBtc(btcAddressSrc, 1, btcWithdrawalConfig.bitcoin.confidenceLevel)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
        integrationHelper.addIrohaAssetTo(testClientSrc, btcAsset, amount.toString())
        integrationHelper.transferAssetIrohaFromClient(
            testClientSrc,
            testClientDestKeypair,
            testClientSrc,
            btcWithdrawalConfig.withdrawalCredential.accountId,
            btcAsset,
            btcAddressDest,
            amount.toString()
        )
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount, createdTransactions.size)
        transactionHelper.addToBlackList(btcAddressSrc)
        transactionHelper.addToBlackList(btcAddressDest)
    }

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given two registered BTC clients. 1st client has 2 BTC in wallet
     * @when 1st client sends 2 BTC to 2nd client without having enough confirmations
     * @then no tx is created, because nobody can use unconfirmed money
     */
    @Test
    fun testWithdrawalNoConfirmedMoney() {
        val initTxCount = createdTransactions.size
        val amount = integrationHelper.btcToSat(2)
        val randomNameSrc = String.getRandomString(9)
        val testClientDestKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClientDestKeypair)
        integrationHelper.sendBtc(btcAddressSrc, 1, btcWithdrawalConfig.bitcoin.confidenceLevel - 1)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
        integrationHelper.addIrohaAssetTo(testClientSrc, btcAsset, amount.toString())
        integrationHelper.transferAssetIrohaFromClient(
            testClientSrc,
            testClientDestKeypair,
            testClientSrc,
            btcWithdrawalConfig.withdrawalCredential.accountId,
            btcAsset,
            btcAddressDest,
            amount.toString()
        )
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount, createdTransactions.size)
        transactionHelper.addToBlackList(btcAddressSrc)
        transactionHelper.addToBlackList(btcAddressDest)
    }

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given two registered BTC clients. 1st client has 10 BTC as 2 unspents(5+5) in wallet.
     * @when 1st client sends BTC 6 to 2nd client
     * @then new well constructed BTC transaction and 2 signatures appear.
     * Transaction is properly signed, sent to Bitcoin network and not considered unsigned anymore
     */
    @Test
    fun testWithdrawalMultipleInputs() {
        val initTxCount = createdTransactions.size
        val amount = integrationHelper.btcToSat(6).toString()
        val randomNameSrc = String.getRandomString(9)
        val testClientDestKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClientDestKeypair)
        integrationHelper.sendBtc(btcAddressSrc, 5, btcWithdrawalConfig.bitcoin.confidenceLevel)
        integrationHelper.sendBtc(btcAddressSrc, 5, btcWithdrawalConfig.bitcoin.confidenceLevel)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
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
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount + 1, createdTransactions.size)
        val createdWithdrawalTx = getLastCreatedTx(createdTransactions)
        signCollector.getSignatures(createdWithdrawalTx).fold({ signatures ->
            logger.info { "signatures $signatures" }
            assertEquals(1, signatures[0]!!.size)
            assertEquals(1, signatures[1]!!.size)
        }, { ex -> fail(ex) })
        transactionHelper.addToBlackList(btcAddressSrc)
        transactionHelper.addToBlackList(btcAddressDest)
        assertFalse(unsignedTransactions.isUnsigned(createdWithdrawalTx))
    }

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given two registered BTC clients. 1st client has 1 BTC in wallet. 1st client has some address in white list.
     * @when 1st client sends SAT 1000 to 2nd client
     * @then no tx is created, because 2nd client is not in 1st client white list
     */
    @Test
    fun testWithdrawalNotWhiteListed() {
        val initTxCount = createdTransactions.size
        val amount = "10000"
        val randomNameSrc = String.getRandomString(9)
        val testClientDestKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(
            randomNameSrc,
            testClientDestKeypair,
            listOf("some_btc_address")
        )
        integrationHelper.sendBtc(btcAddressSrc, 1, 6)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
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
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount, createdTransactions.size)
        transactionHelper.addToBlackList(btcAddressSrc)
        transactionHelper.addToBlackList(btcAddressDest)
    }

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given two registered BTC clients. 1st client has 1 BTC in wallet. 2nd client is in 1st client white list
     * @when 1st client sends SAT 1000 to 2nd client
     * @then new well constructed BTC transaction appears.
     * Transaction is properly signed, sent to Bitcoin network and not considered unsigned anymore
     */
    @Test
    fun testWithdrawalWhiteListed() {
        val initTxCount = createdTransactions.size
        val amount = "10000"
        val randomNameSrc = String.getRandomString(9)
        val testClientDestKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(
            randomNameSrc,
            testClientDestKeypair,
            listOf(btcAddressDest)
        )
        integrationHelper.sendBtc(btcAddressSrc, 1, 6)
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
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount + 1, createdTransactions.size)
        transactionHelper.addToBlackList(btcAddressSrc)
        transactionHelper.addToBlackList(btcAddressDest)
        val createdWithdrawalTx = getLastCreatedTx(createdTransactions)
        assertFalse(unsignedTransactions.isUnsigned(createdWithdrawalTx))
    }

    /**
     * Test US-002 Withdrawal
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given two registered BTC clients. 1st client has 1 BTC in wallet.
     * @when 1st client sends SAT 1000 to 2nd client twice
     * @then only first transaction is well constructed, because there is no unspent transactions left.
     * First transaction is properly signed, sent to Bitcoin network and not considered unsigned anymore
     */
    @Test
    fun testWithdrawalNoUnspentsLeft() {
        val initTxCount = createdTransactions.size
        val amount = "10000"
        val randomNameSrc = String.getRandomString(9)
        val testClientDestKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClientDestKeypair)
        integrationHelper.sendBtc(btcAddressSrc, 1, btcWithdrawalConfig.bitcoin.confidenceLevel)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
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
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount + 1, createdTransactions.size)

        val createdWithdrawalTx = getLastCreatedTx(createdTransactions)
        signCollector.getSignatures(createdWithdrawalTx).fold({ signatures ->
            logger.info { "signatures $signatures" }
            assertEquals(1, signatures[0]!!.size)
        }, { ex -> fail(ex) })
        assertFalse(unsignedTransactions.isUnsigned(createdWithdrawalTx))
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
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount + 1, createdTransactions.size)
        transactionHelper.addToBlackList(btcAddressSrc)
        transactionHelper.addToBlackList(btcAddressDest)
    }

    /**
     * Test US-003 Withdrawal
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given two registered BTC clients. 1st client has no BTC
     * @when 1st client sends SAT 1000 to 2nd client
     * @then no transaction is created, because 1st client has no money at all
     */
    @Test
    fun testWithdrawalNoMoneyWasSentPreviously() {
        val initTxCount = createdTransactions.size
        val amount = "10000"
        val randomNameSrc = String.getRandomString(9)
        val testClientDestKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClientDestKeypair)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
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
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount, createdTransactions.size)
        transactionHelper.addToBlackList(btcAddressDest)
    }

    /**
     * Test US-004 Withdrawal
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given two registered BTC clients. 1st client has 1 BTC
     * @when 1st client sends SAT 10000000000(BTC 100) to 2nd client
     * @then no transaction is created, because 1st client hasn't got enough BTC to spend
     */
    @Test
    fun testWithdrawalNotEnoughMoney() {
        val initTxCount = createdTransactions.size
        val amount = 10_000_000_000
        val randomNameSrc = String.getRandomString(9)
        val testClientDestKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClientDestKeypair)
        integrationHelper.sendBtc(btcAddressSrc, 1, btcWithdrawalConfig.bitcoin.confidenceLevel)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
        integrationHelper.addIrohaAssetTo(testClientSrc, btcAsset, amount.toString())
        integrationHelper.transferAssetIrohaFromClient(
            testClientSrc,
            testClientDestKeypair,
            testClientSrc,
            btcWithdrawalConfig.withdrawalCredential.accountId,
            btcAsset,
            btcAddressDest,
            amount.toString()
        )
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount, createdTransactions.size)
        transactionHelper.addToBlackList(btcAddressSrc)
        transactionHelper.addToBlackList(btcAddressDest)
    }

    private fun getLastCreatedTx(createdTransactions: Map<String, TimedTx>) =
        createdTransactions.maxBy { createdTransactionEntry -> createdTransactionEntry.value.creationTime }!!.key


    private class BlackListableTransactionHelper(
        btcNetworkConfigProvider: BtcNetworkConfigProvider,
        btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider
    ) : TransactionHelper(btcNetworkConfigProvider, btcRegisteredAddressesProvider) {
        //Collection of "blacklisted" addresses. For testing purposes only
        private val btcAddressBlackList = HashSet<String>()

        /**
         * Adds address to black list. It makes given address money unable to spend
         * @param btcAddress - address to add in black list
         */
        fun addToBlackList(btcAddress: String) {
            btcAddressBlackList.add(btcAddress)
        }

        // Checks if transaction output was addressed to available address
        override fun isAvailableOutput(availableAddresses: Set<String>, output: TransactionOutput): Boolean {
            val btcAddress = outPutToBase58Address(output)
            return availableAddresses.contains(btcAddress) && !btcAddressBlackList.contains(btcAddress)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
