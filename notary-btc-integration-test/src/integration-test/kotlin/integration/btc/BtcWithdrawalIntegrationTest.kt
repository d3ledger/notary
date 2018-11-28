package integration.btc

import com.github.kittinunf.result.failure
import helper.address.outPutToBase58Address
import integration.btc.environment.BtcWithdrawalTestEnvironment
import integration.helper.BtcIntegrationHelperUtil
import integration.helper.btcAsset
import jp.co.soramitsu.iroha.ModelCrypto
import mu.KLogging
import org.bitcoinj.core.Address
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertNotNull
import sidechain.iroha.CLIENT_DOMAIN
import util.getRandomString
import withdrawal.btc.transaction.TimedTx
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse

const val WITHDRAWAL_WAIT_MILLIS = 15_000L
private const val TOTAL_TESTS = 9

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcWithdrawalIntegrationTest {
    private val integrationHelper = BtcIntegrationHelperUtil()

    private val environment = BtcWithdrawalTestEnvironment(integrationHelper)

    private lateinit var changeAddress: Address

    @AfterAll
    fun dropDown() {
        environment.close()
    }

    @BeforeAll
    fun setUp() {
        File(environment.btcWithdrawalConfig.bitcoin.blockStoragePath).mkdirs()
        val blockStorageFolder = File(environment.btcWithdrawalConfig.bitcoin.blockStoragePath)
        //Clear bitcoin blockchain folder
        blockStorageFolder.deleteRecursively()
        //Recreate folder
        blockStorageFolder.mkdirs()
        integrationHelper.generateBtcBlocks()
        integrationHelper.genChangeBtcAddress(environment.btcWithdrawalConfig.bitcoin.walletPath)
                .fold({ address -> changeAddress = address }, { ex -> throw  ex })
        integrationHelper.preGenFreeBtcAddresses(environment.btcWithdrawalConfig.bitcoin.walletPath, TOTAL_TESTS * 2)
                .failure { ex -> throw ex }
        environment.btcWithdrawalInitialization.init().failure { ex -> throw ex }
        environment.withdrawalTransferEventHandler.addNewBtcTransactionListener { tx ->
            environment.createdTransactions[tx.hashAsString] = TimedTx.create(tx)
        }
        environment.transactionHelper.addToBlackList(changeAddress.toBase58())
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
        val initTxCount = environment.createdTransactions.size
        val amount = "10000"
        val randomNameSrc = String.getRandomString(9)
        val testClientSrcKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClientSrcKeypair)
        integrationHelper.sendBtc(btcAddressSrc, 1, environment.btcWithdrawalConfig.bitcoin.confidenceLevel)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
        integrationHelper.addIrohaAssetTo(testClientSrc, btcAsset, amount)
        integrationHelper.transferAssetIrohaFromClient(
                testClientSrc,
                testClientSrcKeypair,
                testClientSrc,
                environment.btcWithdrawalConfig.withdrawalCredential.accountId,
                btcAsset,
                btcAddressDest,
                amount
        )
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount + 1, environment.createdTransactions.size)
        val createdWithdrawalTx = environment.getLastCreatedTxHash()
        environment.signCollector.getSignatures(createdWithdrawalTx).fold({ signatures ->
            logger.info { "signatures $signatures" }
            assertEquals(1, signatures[0]!!.size)
        }, { ex -> fail(ex) })
        environment.transactionHelper.addToBlackList(btcAddressSrc)
        environment.transactionHelper.addToBlackList(btcAddressDest)
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

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given two BTC clients are registered after withdrawal service being started. 1st client has 1 BTC in wallet.
     * @when 1st client sends SAT 1000 to 2nd client
     * @then new well constructed BTC transaction and one signature appear.
     * Transaction is properly signed, sent to Bitcoin network and not considered unsigned anymore
     */
    @Test
    fun testWithdrawalAddressGenerationOnFly() {
        val initTxCount = environment.createdTransactions.size
        val amount = "10000"
        val randomNameSrc = String.getRandomString(9)
        val testClientSrcKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddress(
                environment.btcWithdrawalConfig.bitcoin.walletPath,
                randomNameSrc,
                testClientSrcKeypair
        )
        integrationHelper.sendBtc(btcAddressSrc, 1, environment.btcWithdrawalConfig.bitcoin.confidenceLevel)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest =
                integrationHelper.registerBtcAddress(environment.btcWithdrawalConfig.bitcoin.walletPath, randomNameDest)
        integrationHelper.addIrohaAssetTo(testClientSrc, btcAsset, amount)
        integrationHelper.transferAssetIrohaFromClient(
                testClientSrc,
                testClientSrcKeypair,
                testClientSrc,
                environment.btcWithdrawalConfig.withdrawalCredential.accountId,
                btcAsset,
                btcAddressDest,
                amount
        )
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount + 1, environment.createdTransactions.size)
        val createdWithdrawalTx = environment.getLastCreatedTxHash()
        environment.signCollector.getSignatures(createdWithdrawalTx).fold({ signatures ->
            logger.info { "signatures $signatures" }
            assertEquals(1, signatures[0]!!.size)
        }, { ex -> fail(ex) })
        environment.transactionHelper.addToBlackList(btcAddressSrc)
        environment.transactionHelper.addToBlackList(btcAddressDest)
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

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given two registered BTC clients. 1st client has 2 BTC as 2 unspents(1+1) in wallet
     * @when 1st client sends 2 BTC to 2nd client
     * @then no tx is created, because 1st client has no money for fee
     */
    @Test
    fun testWithdrawalNoMoneyLeftForFee() {
        val initTxCount = environment.createdTransactions.size
        val amount = integrationHelper.btcToSat(2)
        val randomNameSrc = String.getRandomString(9)
        val testClienSrcKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClienSrcKeypair)
        integrationHelper.sendBtc(btcAddressSrc, 1, environment.btcWithdrawalConfig.bitcoin.confidenceLevel)
        integrationHelper.sendBtc(btcAddressSrc, 1, environment.btcWithdrawalConfig.bitcoin.confidenceLevel)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
        integrationHelper.addIrohaAssetTo(testClientSrc, btcAsset, amount.toString())
        integrationHelper.transferAssetIrohaFromClient(
                testClientSrc,
                testClienSrcKeypair,
                testClientSrc,
                environment.btcWithdrawalConfig.withdrawalCredential.accountId,
                btcAsset,
                btcAddressDest,
                amount.toString()
        )
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount, environment.createdTransactions.size)
        environment.transactionHelper.addToBlackList(btcAddressSrc)
        environment.transactionHelper.addToBlackList(btcAddressDest)
    }

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given two registered BTC clients. 1st client has 2 BTC in wallet
     * @when 1st client sends 2 BTC to 2nd client without having enough confirmations
     * @then no tx is created, because nobody can use unconfirmed money
     */
    @Test
    fun testWithdrawalNoConfirmedMoney() {
        val initTxCount = environment.createdTransactions.size
        val amount = integrationHelper.btcToSat(2)
        val randomNameSrc = String.getRandomString(9)
        val testClientSrcKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClientSrcKeypair)
        integrationHelper.sendBtc(btcAddressSrc, 1, environment.btcWithdrawalConfig.bitcoin.confidenceLevel - 1)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
        integrationHelper.addIrohaAssetTo(testClientSrc, btcAsset, amount.toString())
        integrationHelper.transferAssetIrohaFromClient(
                testClientSrc,
                testClientSrcKeypair,
                testClientSrc,
                environment.btcWithdrawalConfig.withdrawalCredential.accountId,
                btcAsset,
                btcAddressDest,
                amount.toString()
        )
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount, environment.createdTransactions.size)
        environment.transactionHelper.addToBlackList(btcAddressSrc)
        environment.transactionHelper.addToBlackList(btcAddressDest)
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
        val initTxCount = environment.createdTransactions.size
        val amount = integrationHelper.btcToSat(6).toString()
        val randomNameSrc = String.getRandomString(9)
        val testClientSrcKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClientSrcKeypair)
        integrationHelper.sendBtc(btcAddressSrc, 5, environment.btcWithdrawalConfig.bitcoin.confidenceLevel)
        integrationHelper.sendBtc(btcAddressSrc, 5, environment.btcWithdrawalConfig.bitcoin.confidenceLevel)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
        integrationHelper.addIrohaAssetTo(testClientSrc, btcAsset, amount)
        integrationHelper.transferAssetIrohaFromClient(
                testClientSrc,
                testClientSrcKeypair,
                testClientSrc,
                environment.btcWithdrawalConfig.withdrawalCredential.accountId,
                btcAsset,
                btcAddressDest,
                amount
        )
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount + 1, environment.createdTransactions.size)
        val createdWithdrawalTx = environment.getLastCreatedTxHash()
        environment.signCollector.getSignatures(createdWithdrawalTx).fold({ signatures ->
            logger.info { "signatures $signatures" }
            assertEquals(1, signatures[0]!!.size)
            assertEquals(1, signatures[1]!!.size)
        }, { ex -> fail(ex) })
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
        environment.transactionHelper.addToBlackList(btcAddressSrc)
        environment.transactionHelper.addToBlackList(btcAddressDest)
        assertFalse(environment.unsignedTransactions.isUnsigned(createdWithdrawalTx))
    }

    /**
     * Note: Iroha and bitcoind must be deployed to pass the test.
     * @given two registered BTC clients. 1st client has 1 BTC in wallet. 1st client has some address in white list.
     * @when 1st client sends SAT 1000 to 2nd client
     * @then no tx is created, because 2nd client is not in 1st client white list
     */
    @Test
    fun testWithdrawalNotWhiteListed() {
        val initTxCount = environment.createdTransactions.size
        val amount = "10000"
        val randomNameSrc = String.getRandomString(9)
        val testClientSrcKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(
                randomNameSrc,
                testClientSrcKeypair,
                listOf("some_btc_address")
        )
        integrationHelper.sendBtc(btcAddressSrc, 1, 6)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
        integrationHelper.addIrohaAssetTo(testClientSrc, btcAsset, amount)
        integrationHelper.transferAssetIrohaFromClient(
                testClientSrc,
                testClientSrcKeypair,
                testClientSrc,
                environment.btcWithdrawalConfig.withdrawalCredential.accountId,
                btcAsset,
                btcAddressDest,
                amount
        )
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount, environment.createdTransactions.size)
        environment.transactionHelper.addToBlackList(btcAddressSrc)
        environment.transactionHelper.addToBlackList(btcAddressDest)
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
        val initTxCount = environment.createdTransactions.size
        val amount = "10000"
        val randomNameSrc = String.getRandomString(9)
        val testClientSrcKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(
                randomNameSrc,
                testClientSrcKeypair,
                listOf(btcAddressDest)
        )
        integrationHelper.sendBtc(btcAddressSrc, 1, 6)
        integrationHelper.addIrohaAssetTo(testClientSrc, btcAsset, amount)
        integrationHelper.transferAssetIrohaFromClient(
                testClientSrc,
                testClientSrcKeypair,
                testClientSrc,
                environment.btcWithdrawalConfig.withdrawalCredential.accountId,
                btcAsset,
                btcAddressDest,
                amount
        )
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount + 1, environment.createdTransactions.size)
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
        environment.transactionHelper.addToBlackList(btcAddressSrc)
        environment.transactionHelper.addToBlackList(btcAddressDest)
        val createdWithdrawalTx = environment.getLastCreatedTxHash()
        assertFalse(environment.unsignedTransactions.isUnsigned(createdWithdrawalTx))
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
        val initTxCount = environment.createdTransactions.size
        val amount = "10000"
        val randomNameSrc = String.getRandomString(9)
        val testClientSrcKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClientSrcKeypair)
        integrationHelper.sendBtc(btcAddressSrc, 1, environment.btcWithdrawalConfig.bitcoin.confidenceLevel)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
        integrationHelper.addIrohaAssetTo(testClientSrc, btcAsset, amount)
        integrationHelper.transferAssetIrohaFromClient(
                testClientSrc,
                testClientSrcKeypair,
                testClientSrc,
                environment.btcWithdrawalConfig.withdrawalCredential.accountId,
                btcAsset,
                btcAddressDest,
                amount
        )
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount + 1, environment.createdTransactions.size)

        val createdWithdrawalTx = environment.getLastCreatedTxHash()
        environment.signCollector.getSignatures(createdWithdrawalTx).fold({ signatures ->
            logger.info { "signatures $signatures" }
            assertEquals(1, signatures[0]!!.size)
        }, { ex -> fail(ex) })
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
        assertFalse(environment.unsignedTransactions.isUnsigned(createdWithdrawalTx))
        integrationHelper.addIrohaAssetTo(testClientSrc, btcAsset, amount)
        integrationHelper.transferAssetIrohaFromClient(
                testClientSrc,
                testClientSrcKeypair,
                testClientSrc,
                environment.btcWithdrawalConfig.withdrawalCredential.accountId,
                btcAsset,
                btcAddressDest,
                amount
        )
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount + 1, environment.createdTransactions.size)
        environment.transactionHelper.addToBlackList(btcAddressSrc)
        environment.transactionHelper.addToBlackList(btcAddressDest)
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
        val initTxCount = environment.createdTransactions.size
        val amount = "10000"
        val randomNameSrc = String.getRandomString(9)
        val testClientSrcKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClientSrcKeypair)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
        integrationHelper.addIrohaAssetTo(testClientSrc, btcAsset, amount)
        integrationHelper.transferAssetIrohaFromClient(
                testClientSrc,
                testClientSrcKeypair,
                testClientSrc,
                environment.btcWithdrawalConfig.withdrawalCredential.accountId,
                btcAsset,
                btcAddressDest,
                amount
        )
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount, environment.createdTransactions.size)
        environment.transactionHelper.addToBlackList(btcAddressDest)
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
        val initTxCount = environment.createdTransactions.size
        val amount = 10_000_000_000
        val randomNameSrc = String.getRandomString(9)
        val testClientSrcKeypair = ModelCrypto().generateKeypair()
        val testClientSrc = "$randomNameSrc@$CLIENT_DOMAIN"
        val btcAddressSrc = integrationHelper.registerBtcAddressNoPreGen(randomNameSrc, testClientSrcKeypair)
        integrationHelper.sendBtc(btcAddressSrc, 1, environment.btcWithdrawalConfig.bitcoin.confidenceLevel)
        val randomNameDest = String.getRandomString(9)
        val btcAddressDest = integrationHelper.registerBtcAddressNoPreGen(randomNameDest)
        integrationHelper.addIrohaAssetTo(testClientSrc, btcAsset, amount.toString())
        integrationHelper.transferAssetIrohaFromClient(
                testClientSrc,
                testClientSrcKeypair,
                testClientSrc,
                environment.btcWithdrawalConfig.withdrawalCredential.accountId,
                btcAsset,
                btcAddressDest,
                amount.toString()
        )
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        assertEquals(initTxCount, environment.createdTransactions.size)
        environment.transactionHelper.addToBlackList(btcAddressSrc)
        environment.transactionHelper.addToBlackList(btcAddressDest)
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
