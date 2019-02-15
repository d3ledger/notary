package integration.helper

import config.BitcoinConfig
import config.loadConfigs
import generation.btc.config.BtcAddressGenerationConfig
import model.IrohaCredential
import notary.btc.config.BtcNotaryConfig
import com.d3.btc.registration.config.BtcRegistrationConfig
import withdrawal.btc.config.BtcWithdrawalConfig
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Class that handles all the configuration objects
 **/
class BtcConfigHelper(
    private val accountHelper: IrohaAccountHelper
) : IrohaConfigHelper() {

    /** Creates config for BTC multisig addresses generation */
    fun createBtcAddressGenerationConfig(): BtcAddressGenerationConfig {
        val btcPkPreGenConfig =
            loadConfigs(
                "btc-address-generation",
                BtcAddressGenerationConfig::class.java,
                "/btc/address_generation.properties"
            ).get()

        return object : BtcAddressGenerationConfig {
            override val threshold = 2
            override val changeAddressesStorageAccount = accountHelper.changeAddressesStorageAccount.accountId
            override val healthCheckPort = btcPkPreGenConfig.healthCheckPort
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount.accountId
            override val notaryListSetterAccount = accountHelper.notaryListSetterAccount.accountId
            override val mstRegistrationAccount =
                accountHelper.createCredentialConfig(accountHelper.mstRegistrationAccount)
            override val pubKeyTriggerAccount = btcPkPreGenConfig.pubKeyTriggerAccount
            override val notaryAccount = accountHelper.notaryAccount.accountId
            override val iroha = createIrohaConfig()
            override val btcWalletFilePath = createTempWalletFile(btcPkPreGenConfig.btcWalletFilePath)
            override val registrationAccount = accountHelper.createCredentialConfig(accountHelper.registrationAccount)
        }
    }

    /**
     * Creates config for Bitcoin withdrawal
     * @param testName - name of the test. used to create folder for block storage
     * @return configuration
     */
    fun createBtcWithdrawalConfig(testName: String = ""): BtcWithdrawalConfig {
        val btcWithdrawalConfig =
            loadConfigs("btc-withdrawal", BtcWithdrawalConfig::class.java, "/btc/withdrawal.properties").get()
        return object : BtcWithdrawalConfig {
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
     * Creates config for Bitcoin notary
     * @param testName - name of the test. used to create folder for block storage
     * @param notaryIrohaCredential - notary Iroha credential. Taken from account helper by default
     * @return configuration
     */
    fun createBtcNotaryConfig(
        testName: String = "",
        notaryIrohaCredential: IrohaCredential = accountHelper.notaryAccount
    ): BtcNotaryConfig {
        val btcNotaryConfig = loadConfigs("btc-notary", BtcNotaryConfig::class.java, "/btc/notary.properties").get()
        return object : BtcNotaryConfig {
            override val healthCheckPort = btcNotaryConfig.healthCheckPort
            override val registrationAccount = accountHelper.registrationAccount.accountId
            override val iroha = createIrohaConfig()
            override val bitcoin = createBitcoinConfig(btcNotaryConfig.bitcoin, testName)
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount.accountId
            override val notaryListSetterAccount = accountHelper.notaryListSetterAccount.accountId
            override val notaryCredential = accountHelper.createCredentialConfig(notaryIrohaCredential)
        }
    }

    private fun createBitcoinConfig(bitcoinConfig: BitcoinConfig, testName: String): BitcoinConfig {
        return object : BitcoinConfig {
            override val walletPath = createTempWalletFile(bitcoinConfig.walletPath)
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
            override val healthCheckPort = btcRegistrationConfig.healthCheckPort
            override val notaryAccount = accountHelper.notaryAccount.accountId
            override val mstRegistrationAccount = accountHelper.mstRegistrationAccount.accountId
            override val port = portCounter.incrementAndGet()
            override val registrationCredential =
                accountHelper.createCredentialConfig(accountHelper.registrationAccount)
            override val iroha = createIrohaConfig()
        }
    }

}
