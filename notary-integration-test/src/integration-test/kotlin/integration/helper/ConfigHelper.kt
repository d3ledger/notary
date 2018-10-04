package integration.helper

import config.*
import integration.TestConfig
import notary.btc.config.BtcNotaryConfig
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

    /** Configuration for vacuum service instance */
    val vacuumConfig =
        loadConfigs("relay-vacuum", RelayVacuumConfig::class.java, "/eth/vacuum.properties")

    /** Configuration for registration instance */
    val ethRegistrationConfig =
        loadConfigs("eth-registration", EthRegistrationConfig::class.java, "/eth/registration.properties")

    val btcNotaryConfig = loadConfigs("btc-notary", BtcNotaryConfig::class.java, "/btc/notary.properties")

    val relayRegistrationConfig =
        loadConfigs("relay-registration", RelayRegistrationConfig::class.java, "/eth/relay_registration.properties")

    val btcRegistrationConfig =
        loadConfigs("btc-registration", BtcRegistrationConfig::class.java, "/btc/registration.properties")

    val btcPkPreGenConfig =
        loadConfigs("btc-pregen", BtcPreGenConfig::class.java, "/btc/pregeneration.properties")

    val ethTokenRegistrationConfig =
        loadConfigs(
            "token-registration",
            ERC20TokenRegistrationConfig::class.java,
            "/eth/token_registration.properties"
        )

    //Creates config for ERC20 tokens registration
    fun createERC20TokenRegistrationConfig(tokensFilePath_: String): ERC20TokenRegistrationConfig {
        return object : ERC20TokenRegistrationConfig {
            override val iroha: IrohaConfig
                get() = createIrohaConfig(accountHelper.tokenStorageAccount)
            override val tokensFilePath: String
                get() = tokensFilePath_
            override val tokenStorageAccount: String
                get() = accountHelper.notaryAccount
            override val tokenSetterAccount: String
                get() = accountHelper.tokenStorageAccount
        }
    }

    //Creates config for BTC multisig addresses generation
    fun createBtcPreGenConfig(): BtcPreGenConfig {
        return object : BtcPreGenConfig {
            override val notaryListStorageAccount: String
                get() = accountHelper.notaryListStorageAccount
            override val notaryListSetterAccount: String
                get() = accountHelper.notaryListSetterAccount
            override val mstRegistrationAccount: String
                get() = accountHelper.registrationAccount
            override val pubKeyTriggerAccount: String
                get() = btcPkPreGenConfig.pubKeyTriggerAccount
            override val notaryAccount: String
                get() = accountHelper.notaryAccount
            override val iroha: IrohaConfig
                get() = createIrohaConfig()
            override val btcWalletFilePath: String
                get() = btcPkPreGenConfig.btcWalletFilePath
            override val registrationAccount: String
                get() = accountHelper.registrationAccount
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
                get() = accountHelper.notaryAccount
            override val iroha = createIrohaConfig(accountHelper.registrationAccount)
            override val ethereum: EthereumConfig
                get() = relayRegistrationConfig.ethereum
        }
    }

    /** Test configuration for Iroha */
    fun createIrohaConfig(
        creatorAccount: String = accountHelper.notaryAccount,
        pubkeyPath: String = testConfig.iroha.pubkeyPath,
        privkeyPath: String = testConfig.iroha.privkeyPath
    ): IrohaConfig {
        return object : IrohaConfig {
            override val hostname = testConfig.iroha.hostname
            override val port = testConfig.iroha.port
            override val creator = creatorAccount
            override val pubkeyPath = pubkeyPath
            override val privkeyPath = privkeyPath
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
            override val healthCheckPort: Int
                get() = btcNotaryConfig.healthCheckPort
            override val registrationAccount: String
                get() = accountHelper.registrationAccount
            override val iroha: IrohaConfig
                get() = createIrohaConfig()
            override val bitcoin: BitcoinConfig
                get() = btcNotaryConfig.bitcoin
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount
            override val notaryListSetterAccount = accountHelper.notaryListSetterAccount
        }
    }

    fun createBtcRegistrationConfig(): BtcRegistrationConfig {
        return object : BtcRegistrationConfig {
            override val mstRegistrationAccount: String
                get() = accountHelper.mstRegistrationAccount
            override val port: Int
                get() = btcRegistrationConfig.port
            override val registrationAccount: String
                get() = accountHelper.registrationAccount
            override val iroha: IrohaConfig
                get() = createIrohaConfig()
            override val btcWalletPath: String
                get() = btcRegistrationConfig.btcWalletPath
        }
    }

    /** Test configuration of Notary with runtime dependencies */
    fun createEthNotaryConfig(
        irohaConfig: IrohaConfig = createIrohaConfig(),
        ethereumConfig: EthereumConfig = ethNotaryConfig.ethereum
    ): EthNotaryConfig {
        return object : EthNotaryConfig {
            override val registrationServiceIrohaAccount = accountHelper.registrationAccount
            override val tokenStorageAccount = accountHelper.tokenStorageAccount
            override val tokenSetterAccount = accountHelper.tokenSetterAccount
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount
            override val notaryListSetterAccount = accountHelper.notaryListSetterAccount
            override val whitelistSetter = testConfig.whitelistSetter
            override val refund = createRefundConfig()
            override val iroha = irohaConfig
            override val ethereum = ethereumConfig
        }
    }

    /** Test configuration of Withdrawal service with runtime dependencies */
    fun createWithdrawalConfig(): WithdrawalServiceConfig {
        return object : WithdrawalServiceConfig {
            override val notaryIrohaAccount = accountHelper.notaryAccount
            override val tokenStorageAccount = accountHelper.tokenStorageAccount
            override val tokenSetterAccount = accountHelper.tokenSetterAccount
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount
            override val notaryListSetterAccount = accountHelper.notaryListSetterAccount
            override val registrationIrohaAccount = accountHelper.registrationAccount
            override val iroha = createIrohaConfig()
            override val ethereum = withdrawalConfig.ethereum
        }
    }

    /** Test configuration of Registration with runtime dependencies */
    fun createEthRegistrationConfig(): EthRegistrationConfig {
        return object : EthRegistrationConfig {
            override val port = portCounter.incrementAndGet()
            override val relayRegistrationIrohaAccount = accountHelper.registrationAccount
            override val notaryIrohaAccount = accountHelper.notaryAccount
            override val iroha = createIrohaConfig(accountHelper.registrationAccount)
        }
    }

    fun createRelayVacuumConfig(
        registrationServiceIrohaAccount: String = vacuumConfig.registrationServiceIrohaAccount,
        tokenStorageAccount: String = vacuumConfig.tokenStorageAccount,
        tokenSetterAccount: String = vacuumConfig.tokenSetterAccount
    ): RelayVacuumConfig {
        return object : RelayVacuumConfig {
            override val registrationServiceIrohaAccount = registrationServiceIrohaAccount

            override val tokenStorageAccount = tokenStorageAccount

            override val tokenSetterAccount = tokenSetterAccount

            /** Notary Iroha account that stores relay register */
            override val notaryIrohaAccount = accountHelper.notaryAccount

            /** Iroha configurations */
            override val iroha = vacuumConfig.iroha

            /** Ethereum configurations */
            override val ethereum = vacuumConfig.ethereum
        }
    }

    /**
     * Creates new Ethereum config with given credentials path
     * @param credentialsPath path to Ethereum credentials file (.key)
     * @return EthereumConfig object
     */
    fun createEthereumConfig(credentialsPath: String = ethNotaryConfig.ethereum.credentialsPath): EthereumConfig {
        return object : EthereumConfig {
            override val confirmationPeriod = ethNotaryConfig.ethereum.confirmationPeriod
            override val credentialsPath = credentialsPath
            override val gasLimit = ethNotaryConfig.ethereum.gasLimit
            override val gasPrice = ethNotaryConfig.ethereum.gasPrice
            override val url = ethNotaryConfig.ethereum.url
        }
    }

    companion object {
        /** Port counter, so new port is generated for each run */
        private val portCounter = AtomicInteger(19_999)
    }
}
