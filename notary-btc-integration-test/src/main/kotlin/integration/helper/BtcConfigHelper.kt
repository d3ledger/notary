package integration.helper

import com.d3.btc.deposit.config.BtcDepositConfig
import com.d3.btc.generation.config.BtcAddressGenerationConfig
import com.d3.btc.registration.config.BtcRegistrationConfig
import com.d3.btc.withdrawal.config.BtcWithdrawalConfig
import config.BitcoinConfig
import config.loadConfigs
import model.IrohaCredential
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Class that handles all the configuration objects
 **/
class BtcConfigHelper(
    private val accountHelper: IrohaAccountHelper
) : IrohaConfigHelper() {

    /** Creates config for BTC multisig addresses generation
     * @param initAddresses - number of addresses that will be generated at initial phase
     * @return config
     * */
    fun createBtcAddressGenerationConfig(initAddresses: Int): BtcAddressGenerationConfig {
        val btcPkPreGenConfig =
            loadConfigs(
                "btc-address-generation",
                BtcAddressGenerationConfig::class.java,
                "/btc/address_generation.properties"
            ).get()

        return object : BtcAddressGenerationConfig {
            override val threshold = initAddresses
            override val nodeId = NODE_ID
            override val changeAddressesStorageAccount = accountHelper.changeAddressesStorageAccount.accountId
            override val healthCheckPort = btcPkPreGenConfig.healthCheckPort
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount.accountId
            override val notaryListSetterAccount = accountHelper.notaryListSetterAccount.accountId
            override val mstRegistrationAccount =
                accountHelper.createCredentialConfig(accountHelper.mstRegistrationAccount)
            override val pubKeyTriggerAccount = btcPkPreGenConfig.pubKeyTriggerAccount
            override val notaryAccount = accountHelper.notaryAccount.accountId
            override val iroha = createIrohaConfig()
            override val btcKeysWalletPath = createTempWalletFile(btcPkPreGenConfig.btcKeysWalletPath)
            override val registrationAccount = accountHelper.createCredentialConfig(accountHelper.registrationAccount)
        }
    }

    /**
     * Creates config for Bitcoin withdrawal
     * @param testName - name of the test. used to create folder for block storage and queue name
     * @return configuration
     */
    fun createBtcWithdrawalConfig(testName: String = ""): BtcWithdrawalConfig {
        val btcWithdrawalConfig =
            loadConfigs("btc-withdrawal", BtcWithdrawalConfig::class.java, "/btc/withdrawal.properties").get()
        return object : BtcWithdrawalConfig {
            override val irohaBlockQueue = testName
            override val btcKeysWalletPath = createTempWalletFile(btcWithdrawalConfig.btcKeysWalletPath)
            override val btcTransfersWalletPath = createTempWalletFile(btcWithdrawalConfig.btcTransfersWalletPath)
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount.accountId
            override val notaryListSetterAccount = accountHelper.notaryListSetterAccount.accountId
            override val btcFeeRateCredential = accountHelper.createCredentialConfig(accountHelper.btcFeeRateAccount)
            override val signatureCollectorCredential =
                accountHelper.createCredentialConfig(accountHelper.btcWithdrawalSignatureCollectorAccount)
            override val changeAddressesStorageAccount = accountHelper.changeAddressesStorageAccount.accountId
            override val registrationCredential =
                accountHelper.createCredentialConfig(accountHelper.registrationAccount)
            override val mstRegistrationAccount = accountHelper.mstRegistrationAccount.accountId
            override val bitcoin = createBitcoinConfig(btcWithdrawalConfig.bitcoin, testName)
            override val notaryCredential = accountHelper.createCredentialConfig(accountHelper.notaryAccount)
            override val healthCheckPort = btcWithdrawalConfig.healthCheckPort
            override val withdrawalCredential = accountHelper.createCredentialConfig(accountHelper.btcWithdrawalAccount)
            override val iroha = btcWithdrawalConfig.iroha
        }
    }

    /*
        Creates temporary copy of wallet file just for testing
     */
    fun createTempWalletFile(btcWalletPath: String): String {
        val newWalletFilePath = btcWalletPath.replaceFirst(".wallet", "-test.wallet")
        val walletFile = File(newWalletFilePath)
        if (!walletFile.exists()) {
            FileInputStream(btcWalletPath).use { src ->
                FileOutputStream(newWalletFilePath).use { dest ->
                    val srcChannel = src.channel
                    dest.channel.transferFrom(srcChannel, 0, srcChannel.size())
                }
            }
        }
        return newWalletFilePath
    }

    /**
     * Creates config for Bitcoin deposit
     * @param testName - name of the test. used to create folder for block storage
     * @param notaryIrohaCredential - notary Iroha credential. Taken from account helper by default
     * @return configuration
     */
    fun createBtcDepositConfig(
        testName: String = "",
        notaryIrohaCredential: IrohaCredential = accountHelper.notaryAccount
    ): BtcDepositConfig {
        val btcDepositConfig = loadConfigs("btc-deposit", BtcDepositConfig::class.java, "/btc/deposit.properties").get()
        return object : BtcDepositConfig {
            override val btcTransferWalletPath = createTempWalletFile(btcDepositConfig.btcTransferWalletPath)
            override val healthCheckPort = btcDepositConfig.healthCheckPort
            override val registrationAccount = accountHelper.registrationAccount.accountId
            override val iroha = createIrohaConfig()
            override val bitcoin = createBitcoinConfig(btcDepositConfig.bitcoin, testName)
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount.accountId
            override val notaryListSetterAccount = accountHelper.notaryListSetterAccount.accountId
            override val notaryCredential = accountHelper.createCredentialConfig(notaryIrohaCredential)
        }
    }

    private fun createBitcoinConfig(bitcoinConfig: BitcoinConfig, testName: String): BitcoinConfig {
        return object : BitcoinConfig {
            override val blockStoragePath = createTempBlockStorageFolder(bitcoinConfig.blockStoragePath, testName)
            override val confidenceLevel = bitcoinConfig.confidenceLevel
            override val hosts = bitcoinConfig.hosts
        }
    }

    /*
    Creates temporary folder for Bitcoin block storage
    */
    fun createTempBlockStorageFolder(btcBlockStorageFolder: String, postFix: String): String {
        val newBlockStorageFolder =
            if (postFix.isEmpty()) {
                btcBlockStorageFolder
            } else {
                "${btcBlockStorageFolder}_$postFix"
            }
        val blockStorageFolder = File(newBlockStorageFolder)
        if (!blockStorageFolder.exists()) {
            if (!blockStorageFolder.mkdirs()) {
                throw IllegalStateException("Cannot create folder '$blockStorageFolder' for block storage")
            }
        }
        return newBlockStorageFolder
    }

    fun createBtcRegistrationConfig(): BtcRegistrationConfig {
        val btcRegistrationConfig =
            loadConfigs("btc-registration", BtcRegistrationConfig::class.java, "/btc/registration.properties").get()
        return object : BtcRegistrationConfig {
            override val nodeId = NODE_ID
            override val notaryAccount = accountHelper.notaryAccount.accountId
            override val mstRegistrationAccount = accountHelper.mstRegistrationAccount.accountId
            override val port = portCounter.incrementAndGet()
            override val registrationCredential =
                accountHelper.createCredentialConfig(accountHelper.registrationAccount)
            override val iroha = createIrohaConfig()
        }
    }
}
