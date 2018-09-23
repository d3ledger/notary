package integration.helper

import config.*
import model.IrohaCredential
import notary.btc.BtcNotaryConfig
import notary.eth.EthNotaryConfig
import notary.eth.RefundConfig
import registration.btc.BtcRegistrationConfig
import registration.btc.pregen.BtcPreGenConfig
import registration.eth.EthRegistrationConfig
import registration.eth.relay.RelayRegistrationConfig
import token.ERC20TokenRegistrationConfig
import vacuum.RelayVacuumConfig
import withdrawalservice.WithdrawalServiceConfig
import java.util.concurrent.atomic.AtomicInteger

//Class that handles all the configuration objects.
class ConfigHelper(private val accountHelper: AccountHelper) {

    /** Configurations for tests */
    val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties")

    /** Ethereum password configs */
    val ethPasswordConfig = loadEthPasswords("test", "/eth/ethereum_password.properties")

    /** Configuration for notary instance */
    val ethNotaryConfig = loadConfigs("eth-notary", EthNotaryConfig::class.java, "/eth/notary.properties")

    /** Configuration for withdrawal service instance */
    val withdrawalConfig =
        loadConfigs("withdrawal", WithdrawalServiceConfig::class.java, "/eth/withdrawal.properties")

    /** Configuration for registration instance */
    val ethRegistrationConfig =
        loadConfigs("eth-registration", EthRegistrationConfig::class.java, "/eth/registration.properties")

    val btcNotaryConfig = loadConfigs("btc-notary", BtcNotaryConfig::class.java, "/btc/notary.properties")

    val relayRegistrationConfig =
        loadConfigs("test", RelayRegistrationConfig::class.java, "/test.properties")

    val btcRegistrationConfig =
        loadConfigs("btc-registration", BtcRegistrationConfig::class.java, "/btc/registration.properties")

    val btcPkPreGenConfig =
        loadConfigs("btc-pregen", BtcPreGenConfig::class.java, "/btc/pregeneration.properties")

    val ethTokenRegistrationConfig =
        loadConfigs("token-registration", ERC20TokenRegistrationConfig::class.java, "/eth/token_registration.properties")

    fun getTestCredentialConfig(): IrohaCredentialConfig {
        return testConfig.testCredential
    }

    //Creates config for ERC20 tokens registration
    fun createERC20TokenRegistrationConfig(tokensFilePath_: String): ERC20TokenRegistrationConfig {
        return object : ERC20TokenRegistrationConfig {
            override val iroha: IrohaConfig
                get() = createIrohaConfig()
            override val tokensFilePath: String
                get() = tokensFilePath_
            override val tokenStorageAccount: String
                get() = accountHelper.notaryAccount.accountId
            override val tokenCreatorAccount: IrohaCredentialConfig
                get() =  getTestCredentialConfig()
        }
    }

    //Creates config for BTC multisig addresses generation
    fun createBtcPreGenConfig(): BtcPreGenConfig {
        return object : BtcPreGenConfig {
            override val notaryListStorageAccount: String
                get() = accountHelper.notaryListStorageAccount.accountId
            override val notaryListSetterAccount: String
                get() = accountHelper.notaryListSetterAccount.accountId
            override val mstRegistrationAccount: IrohaCredentialConfig
                get() =  getTestCredentialConfig()
            override val pubKeyTriggerAccount: String
                get() = btcPkPreGenConfig.pubKeyTriggerAccount
            override val notaryAccount: String
                get() = accountHelper.notaryAccount.accountId
            override val iroha: IrohaConfig
                get() = createIrohaConfig()
            override val btcWalletFilePath: String
                get() = btcPkPreGenConfig.btcWalletFilePath
            override val registrationAccount: IrohaCredentialConfig
                get() = getTestCredentialConfig()
        }
    }

    //Creates config for ETH relays registration
    fun createRelayRegistrationConfig(): RelayRegistrationConfig {
        return object : RelayRegistrationConfig {
            override val number: Int
                get() = relayRegistrationConfig.number
            override val ethMasterWallet: String
                get() = relayRegistrationConfig.ethMasterWallet
            override val notaryIrohaAccount: String
                get() = accountHelper.notaryAccount.accountId
            override val iroha = createIrohaConfig()
            override val ethereum: EthereumConfig
                get() = relayRegistrationConfig.ethereum
            override val relayRegistrationCredential: IrohaCredentialConfig
                get() = getTestCredentialConfig()
        }
    }

    /** Test configuration for Iroha */
    fun createIrohaConfig(
    ): IrohaConfig {
        return object : IrohaConfig {
            override val hostname = testConfig.iroha.hostname
            override val port = testConfig.iroha.port
        }
    }

    /**
     * Test configuration for refund endpoint
     * Create unique port for refund for every call
     */
    fun createRefundConfig(): RefundConfig {
        return object : RefundConfig {
            override val endpointEthereum = ethNotaryConfig.refund.endpointEthereum
            override val port = portCounter.incrementAndGet()
        }
    }

    fun createBtcNotaryConfig(): BtcNotaryConfig {
        return object : BtcNotaryConfig {
            override val registrationAccount: String
                get() = accountHelper.registrationAccount.accountId
            override val iroha: IrohaConfig
                get() = createIrohaConfig()
            override val bitcoin: BitcoinConfig
                get() = btcNotaryConfig.bitcoin
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount.accountId
            override val notaryListSetterAccount = accountHelper.notaryListSetterAccount.accountId
            override val notaryCredential = getTestCredentialConfig()
        }
    }

    fun createBtcRegistrationConfig(): BtcRegistrationConfig {
        return object : BtcRegistrationConfig {
            override val mstRegistrationAccount: String
                get() = accountHelper.mstRegistrationAccount.accountId
            override val port: Int
                get() = btcRegistrationConfig.port
            override val registrationCredential: IrohaCredentialConfig
                get() =  getTestCredentialConfig()
            override val iroha: IrohaConfig
                get() = createIrohaConfig()
            override val btcWalletPath: String
                get() = btcRegistrationConfig.btcWalletPath
        }
    }

    /** Test configuration of Notary with runtime dependencies */
    fun createEthNotaryConfig(irohaConfig: IrohaConfig = createIrohaConfig()): EthNotaryConfig {
        return object : EthNotaryConfig {
            override val registrationServiceIrohaAccount = accountHelper.registrationAccount.accountId
            override val tokenStorageAccount = accountHelper.tokenStorageAccount.accountId
            override val tokenSetterAccount = accountHelper.tokenSetterAccount.accountId
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount.accountId
            override val notaryListSetterAccount = accountHelper.notaryListSetterAccount.accountId
            override val whitelistSetter = testConfig.whitelistSetter
            override val notaryCredential =  getTestCredentialConfig()
            override val refund = createRefundConfig()
            override val iroha = irohaConfig
            override val ethereum = ethNotaryConfig.ethereum
        }
    }

    /** Test configuration of Withdrawal service with runtime dependencies */
    fun createWithdrawalConfig(): WithdrawalServiceConfig {
        return object : WithdrawalServiceConfig {
            override val notaryIrohaAccount = accountHelper.notaryAccount.accountId
            override val tokenStorageAccount = accountHelper.tokenStorageAccount.accountId
            override val tokenSetterAccount = accountHelper.tokenSetterAccount.accountId
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount.accountId
            override val notaryListSetterAccount = accountHelper.notaryListSetterAccount.accountId
            override val registrationIrohaAccount = accountHelper.registrationAccount.accountId
            override val withdrawalCredential =  getTestCredentialConfig()
            override val iroha = createIrohaConfig()
            override val ethereum = withdrawalConfig.ethereum
        }
    }

    /** Test configuration of Registration with runtime dependencies */
    fun createEthRegistrationConfig(): EthRegistrationConfig {
        return object : EthRegistrationConfig {
            override val port = portCounter.incrementAndGet()
            override val relayRegistrationIrohaAccount = accountHelper.registrationAccount.accountId
            override val notaryIrohaAccount = accountHelper.notaryAccount.accountId
            override val iroha = createIrohaConfig()
            override val registrationCredential = getTestCredentialConfig()
        }
    }

    fun createRelayVacuumConfig(): RelayVacuumConfig {
        return object : RelayVacuumConfig {
            override val registrationServiceIrohaAccount = accountHelper.registrationAccount.accountId

            override val tokenStorageAccount = accountHelper.tokenStorageAccount.accountId

            /** Notary Iroha account that stores relay register */
            override val notaryIrohaAccount = accountHelper.notaryAccount.accountId

            override val vacuumCredential = getTestCredentialConfig()
            /** Iroha configurations */
            override val iroha = createIrohaConfig()

            /** Ethereum configurations */
            override val ethereum = testConfig.ethereum
        }
    }

    companion object {
        /** Port counter, so new port is generated for each run */
        private val portCounter = AtomicInteger(19_999)
    }
}
