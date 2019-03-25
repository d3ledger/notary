/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.helper

import com.d3.btc.deposit.config.BtcDepositConfig
import com.d3.btc.generation.config.BtcAddressGenerationConfig
import com.d3.btc.registration.config.BtcRegistrationConfig
import com.d3.btc.withdrawal.config.BtcWithdrawalConfig
import com.d3.commons.config.BitcoinConfig
import com.d3.commons.config.IrohaCredentialConfig
import com.d3.commons.config.loadConfigs
import com.d3.commons.model.IrohaCredential
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.wallet.Wallet
import java.io.File

private const val TEST_WALLETS_FOLDER = "deploy/bitcoin/regtest/test wallets"

/**
 * Class that handles all the configuration objects
 **/
class BtcConfigHelper(
    private val accountHelper: IrohaAccountHelper
) : IrohaConfigHelper() {

    /** Creates config for BTC multisig addresses generation
     * @param initAddresses - number of addresses that will be generated at initial phase
     * @param walletNamePostfix - postfix of wallet file name. used to keep wallets as multiple files in multisig tests.
     * @return config
     * */
    fun createBtcAddressGenerationConfig(
        initAddresses: Int,
        walletNamePostfix: String = "test"
    ): BtcAddressGenerationConfig {
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
            override val btcKeysWalletPath = createWalletFile("keys.$walletNamePostfix")
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
            override val btcConsensusCredential =
                accountHelper.createCredentialConfig(accountHelper.btcConsensusAccount)
            override val irohaBlockQueue = testName
            override val btcKeysWalletPath = createWalletFile("keys.$testName")
            override val btcTransfersWalletPath = createWalletFile("transfers.$testName")
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
        Creates wallet file just for testing
     */
    fun createWalletFile(walletNamePostfix: String): String {
        val testWalletsFolder = File(TEST_WALLETS_FOLDER)
        if (!testWalletsFolder.exists()) {
            testWalletsFolder.mkdirs()
        }
        val newWalletFilePath = "$TEST_WALLETS_FOLDER/d3.$walletNamePostfix.wallet"
        Wallet(RegTestParams.get()).saveToFile(File(newWalletFilePath))
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
            override val btcTransferWalletPath = createWalletFile("transfers.$testName")
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
