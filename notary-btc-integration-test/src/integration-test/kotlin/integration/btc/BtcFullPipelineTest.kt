package integration.btc

import com.github.kittinunf.result.failure
import helper.currency.satToBtc
import integration.btc.environment.BtcAddressGenerationTestEnvironment
import integration.btc.environment.BtcNotaryTestEnvironment
import integration.btc.environment.BtcRegistrationTestEnvironment
import integration.btc.environment.BtcWithdrawalTestEnvironment
import integration.helper.BTC_ASSET
import integration.helper.BtcIntegrationHelperUtil
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KLogging
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import provider.btc.address.BtcAddressType
import sidechain.iroha.CLIENT_DOMAIN
import util.getRandomString
import util.hex
import withdrawal.btc.handler.CurrentFeeRate
import java.io.File
import java.math.BigDecimal
import java.security.KeyPair

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BtcFullPipelineTest {

    private val integrationHelper = BtcIntegrationHelperUtil()

    private val addressGenerationEnvironment = BtcAddressGenerationTestEnvironment(integrationHelper)

    private val registrationEnvironment = BtcRegistrationTestEnvironment(integrationHelper)

    private val notaryEnvironment = BtcNotaryTestEnvironment(integrationHelper, "full_pipeline")

    private val withdrawalEnvironment = BtcWithdrawalTestEnvironment(integrationHelper, "full_pipeline")

    init {
        CurrentFeeRate.set(DEFAULT_FEE_RATE)
        integrationHelper.addNotary("test_notary", "test_notary_address")
        // Run address generation
        GlobalScope.launch {
            addressGenerationEnvironment.btcAddressGenerationInitialization.init().failure { ex -> throw ex }
        }
        // Wait for initial address generation
        Thread.sleep(WAIT_PREGEN_PROCESS_MILLIS * addressGenerationEnvironment.btcGenerationConfig.threshold)
        addressGenerationEnvironment.checkIfAddressesWereGeneratedAtInitialPhase()
        // Run registration
        GlobalScope.launch {
            registrationEnvironment.btcRegistrationServiceInitialization.init()
        }

        integrationHelper.generateBtcInitialBlocks()

        // Run notary
        GlobalScope.launch {
            val blockStorageFolder = File(notaryEnvironment.notaryConfig.bitcoin.blockStoragePath)
            //Clear bitcoin blockchain folder
            blockStorageFolder.deleteRecursively()
            //Recreate folder
            blockStorageFolder.mkdirs()
            notaryEnvironment.btcNotaryInitialization.init().failure { ex -> fail("Cannot run BTC notary", ex) }
        }

        generateChangeAddress()

        // Run withdrawal
        GlobalScope.launch {
            File(withdrawalEnvironment.btcWithdrawalConfig.bitcoin.blockStoragePath).mkdirs()
            val blockStorageFolder = File(withdrawalEnvironment.btcWithdrawalConfig.bitcoin.blockStoragePath)
            //Clear bitcoin blockchain folder
            blockStorageFolder.deleteRecursively()
            //Recreate folder
            blockStorageFolder.mkdirs()
            withdrawalEnvironment.btcWithdrawalInitialization.init().failure { ex -> throw ex }
        }

        Thread.sleep(10_000)
    }

    @AfterAll
    fun closeEnvironments() {
        addressGenerationEnvironment.close()
        registrationEnvironment.close()
        notaryEnvironment.close()
        withdrawalEnvironment.close()
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given all the services(notary, withdrawal, registration and address generation) are running. 2 clients are registered. 1st client has 1BTC.
     * @when 1st client sends 10000 SAT to 2nd client
     * @then 10000 SAT is subtracted from 1st client balance and 2nd client balance is increased by 10000 SAT
     */
    @Test
    fun testFullPipeline() {
        val amount = satToBtc(10000L)

        // Register source account
        val srcKeypair = Ed25519Sha3().generateKeypair()
        val srcUserName = "src_${String.getRandomString(9)}"
        val srcBtcAddress = registerClient(srcUserName, srcKeypair)

        // Register destination account
        val destKeypair = Ed25519Sha3().generateKeypair()
        val destUserName = "dest_${String.getRandomString(9)}"
        val destBtcAddress = registerClient(destUserName, destKeypair)

        // Send 1 BTC
        integrationHelper.sendBtc(srcBtcAddress, 1, notaryEnvironment.notaryConfig.bitcoin.confidenceLevel)
        Thread.sleep(DEPOSIT_WAIT_MILLIS)

        // Send 10000 SAT from source to destination
        integrationHelper.transferAssetIrohaFromClient(
            "$srcUserName@$CLIENT_DOMAIN",
            srcKeypair,
            "$srcUserName@$CLIENT_DOMAIN",
            withdrawalEnvironment.btcWithdrawalConfig.withdrawalCredential.accountId,
            BTC_ASSET,
            destBtcAddress,
            amount.toPlainString()
        )
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS)
        integrationHelper.generateBtcBlocks(notaryEnvironment.notaryConfig.bitcoin.confidenceLevel)
        Thread.sleep(DEPOSIT_WAIT_MILLIS)
        assertEquals(
            amount.toPlainString(),
            integrationHelper.getIrohaAccountBalance("$destUserName@$CLIENT_DOMAIN", BTC_ASSET)
        )
        assertEquals(
            BigDecimal(1).subtract(amount).toPlainString(),
            integrationHelper.getIrohaAccountBalance("$srcUserName@$CLIENT_DOMAIN", BTC_ASSET)
        )

        addressGenerationEnvironment.btcFreeAddressesProvider.getFreeAddresses()
            .fold({ freeAddresses ->
                if (freeAddresses.size < addressGenerationEnvironment.btcGenerationConfig.threshold) {
                    fail(
                        "Not enough addresses were generated after registration" +
                                "(${freeAddresses.size} out of ${addressGenerationEnvironment.btcGenerationConfig.threshold})."
                    )
                }
            }, { ex -> fail("Cannot get free addresses", ex) })
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given all the services(notary, withdrawal, registration and address generation) are running. 2 clients are registered. 1st client has 1BTC.
     * @when 1st client sends 10000 SAT to 2nd client multiple times
     * @then 10000 SAT is subtracted from 1st client balance and 2nd client balance is increased by 10000 SAT multiple times
     */
    @Test
    fun testFullPipelineMultipleTransfers() {
        val totalTransfers = 5
        val amount = satToBtc(10000L)

        // Register source account
        val srcKeypair = Ed25519Sha3().generateKeypair()
        val srcUserName = "src_${String.getRandomString(9)}"
        val srcBtcAddress = registerClient(srcUserName, srcKeypair)

        // Register destination account
        val destKeypair = Ed25519Sha3().generateKeypair()
        val destUserName = "dest_${String.getRandomString(9)}"
        val destBtcAddress = registerClient(destUserName, destKeypair)

        // Send 1 BTC multiple times
        for (transfer in 1..totalTransfers) {
            Thread {
                integrationHelper.sendBtc(
                    srcBtcAddress,
                    1,
                    notaryEnvironment.notaryConfig.bitcoin.confidenceLevel
                )
            }.start()
        }
        Thread.sleep(DEPOSIT_WAIT_MILLIS * totalTransfers)
        val createdTime = System.currentTimeMillis()
        // Send 10000 SAT from source to destination multiple times
        for (transfer in 1..totalTransfers) {
            Thread {
                integrationHelper.transferAssetIrohaFromClient(
                    "$srcUserName@$CLIENT_DOMAIN",
                    srcKeypair,
                    "$srcUserName@$CLIENT_DOMAIN",
                    withdrawalEnvironment.btcWithdrawalConfig.withdrawalCredential.accountId,
                    BTC_ASSET,
                    destBtcAddress,
                    amount.toPlainString(),
                    /**
                     * Every transfer transaction must have different creation time
                     * Otherwise some transactions may have the same tx hash
                     * */
                    createdTime + transfer
                )
            }.start()
        }
        Thread.sleep(WITHDRAWAL_WAIT_MILLIS * totalTransfers)
        integrationHelper.generateBtcBlocks(notaryEnvironment.notaryConfig.bitcoin.confidenceLevel)
        Thread.sleep(DEPOSIT_WAIT_MILLIS * totalTransfers)
        assertEquals(
            amount.multiply(BigDecimal(totalTransfers)).toPlainString(),
            integrationHelper.getIrohaAccountBalance("$destUserName@$CLIENT_DOMAIN", BTC_ASSET)
        )
        assertEquals(
            BigDecimal(totalTransfers).subtract(amount.multiply(BigDecimal(totalTransfers))).toPlainString(),
            integrationHelper.getIrohaAccountBalance("$srcUserName@$CLIENT_DOMAIN", BTC_ASSET)
        )
    }

    /**
     * Registers Iroha client
     * @param userName - name of user to register
     * @param keypair - registered user key pair
     * @return registered Bitcoin address
     */
    private fun registerClient(userName: String, keypair: KeyPair): String {
        val res = khttp.post(
            "http://127.0.0.1:${registrationEnvironment.btcRegistrationConfig.port}/users",
            data = mapOf("name" to userName, "pubkey" to String.hex(keypair.public.encoded), "whitelist" to "")
        )
        return String(res.content)
    }

    /**
     * Generates Bitcoin address to store changes
     */
    private fun generateChangeAddress() {
        generateAddress(BtcAddressType.CHANGE)
    }

    /**
     * Starts address generation process
     * @param addressType - type of Bitcoin address to generate
     */
    private fun generateAddress(addressType: BtcAddressType) {
        val sessionAccountName = addressType.createSessionAccountName()
        addressGenerationEnvironment.btcKeyGenSessionProvider.createPubKeyCreationSession(sessionAccountName)
            .fold({ logger.info { "session $sessionAccountName was created" } },
                { ex -> fail("cannot create session", ex) })
        addressGenerationEnvironment.triggerProvider.trigger(sessionAccountName)
        Thread.sleep(WAIT_PREGEN_PROCESS_MILLIS)
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
