package integration.btc

import com.github.kittinunf.result.failure
import integration.helper.IntegrationHelperUtil
import integration.helper.btcAsset
import jp.co.soramitsu.iroha.ModelCrypto
import model.IrohaCredential
import org.bitcoinj.core.Address
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import provider.btc.address.BtcRegisteredAddressesProvider
import provider.btc.network.BtcRegTestConfigProvider
import sidechain.iroha.CLIENT_DOMAIN
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import withdrawal.btc.BtcWithdrawalInitialization
import withdrawal.transaction.TransactionCreator
import withdrawal.transaction.TransactionHelper
import java.io.File
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcWithdrawalIntegrationTest {
    private val integrationHelper = IntegrationHelperUtil()

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
    private val transactionHelper = TransactionHelper(btcNetworkConfigProvider, btcRegisteredAddressesProvider)
    private val transactionCreator = TransactionCreator(changeAddress, btcNetworkConfigProvider, transactionHelper)
    private val btcWithdrawalInitialization =
        BtcWithdrawalInitialization(
            btcWithdrawalConfig,
            irohaChainListener,
            transactionCreator,
            btcNetworkConfigProvider
        )

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
        irohaChainListener.close()
    }

    @BeforeAll
    fun setUp() {
        integrationHelper.generateBtcBlocks()
        integrationHelper.preGenBtcAddresses(btcWithdrawalConfig.bitcoin.walletPath, 10).failure { ex -> throw ex }
        btcWithdrawalInitialization.init()
    }

    /**
     * Test US-001 Withdrawal
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given two registered BTC clients. 1st client has 1 BTC in wallet.
     * @when 1st client sends SAT 1000 to 2nd client
     * @then new well constructed BTC transaction appears
     */
    @Test
    fun testWithdrawal() {
        val initTxCount = btcWithdrawalInitialization.getUnsignedTransactions().size
        val amount = "10000"
        val randomNameSrc = String.getRandomString(9)
        val testClientDestKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClientDestKeypair)
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
        Thread.sleep(5_000)
        assertEquals(initTxCount + 1, btcWithdrawalInitialization.getUnsignedTransactions().size)
    }

    /**
     * Test US-002 Withdrawal
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given two registered BTC clients. 1st client has 1 BTC in wallet.
     * @when 1st client sends SAT 1000 to 2nd client twice
     * @then only first transaction is well constructed, because there is no unspent transactions left
     */
    @Test
    fun testWithdrawalNoUnspentsLeft() {
        val initTxCount = btcWithdrawalInitialization.getUnsignedTransactions().size
        val amount = "10000"
        val randomNameSrc = String.getRandomString(9)
        val testClientDestKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClientDestKeypair)
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
        Thread.sleep(5_000)
        assertEquals(initTxCount + 1, btcWithdrawalInitialization.getUnsignedTransactions().size)

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
        Thread.sleep(5_000)
        assertEquals(initTxCount + 1, btcWithdrawalInitialization.getUnsignedTransactions().size)
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
        val initTxCount = btcWithdrawalInitialization.getUnsignedTransactions().size
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
        Thread.sleep(5_000)
        assertEquals(initTxCount, btcWithdrawalInitialization.getUnsignedTransactions().size)
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
        val initTxCount = btcWithdrawalInitialization.getUnsignedTransactions().size
        val amount = 10_000_000_000
        val randomNameSrc = String.getRandomString(9)
        val testClientDestKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClientDestKeypair)
        integrationHelper.sendBtc(btcAddressSrc, 1, 6)
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
        Thread.sleep(5_000)
        assertEquals(initTxCount, btcWithdrawalInitialization.getUnsignedTransactions().size)
    }
}
