package integration.helper

import config.*
import notary.btc.BtcNotaryConfig
import notary.eth.EthNotaryConfig
import notary.eth.RefundConfig
import registration.btc.BtcRegistrationConfig
import registration.eth.EthRegistrationConfig
import registration.eth.relay.RelayRegistrationConfig
import vacuum.RelayVacuumConfig
import withdrawalservice.WithdrawalServiceConfig
import java.util.concurrent.atomic.AtomicInteger

class ConfigHelper(private val accountHelper: AccountHelper) {
    /** Configurations for tests */
    val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties")

    /** Ethereum password configs */
    val ethPasswordConfig = loadConfigs("test", EthereumPasswords::class.java, "/eth/ethereum_password.properties")

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


    fun createRelayRegistrationConfig(): RelayRegistrationConfig {
        return object : RelayRegistrationConfig {
            override val number: Int
                get() = relayRegistrationConfig.number
            override val ethMasterWallet: String
                get() = relayRegistrationConfig.ethMasterWallet
            override val notaryIrohaAccount: String
                get() = accountHelper.notaryAccount
            override val iroha: IrohaConfig
                get() = createIrohaConfig()
            override val ethereum: EthereumConfig
                get() = relayRegistrationConfig.ethereum
        }
    }

    /** Test configuration for Iroha */
    fun createIrohaConfig(creatorAccount: String = accountHelper.notaryAccount): IrohaConfig {
        return object : IrohaConfig {
            override val hostname: String
                get() = testConfig.iroha.hostname
            override val port: Int
                get() = testConfig.iroha.port
            override val creator: String
                get() = creatorAccount
            override val pubkeyPath: String
                get() = testConfig.iroha.pubkeyPath
            override val privkeyPath: String
                get() = testConfig.iroha.privkeyPath
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
            override val notaryIrohaAccount: String
                get() = accountHelper.notaryAccount
            override val iroha: IrohaConfig
                get() = createIrohaConfig()
            override val bitcoin: BitcoinConfig
                get() = btcNotaryConfig.bitcoin
        }
    }

    fun createBtcRegistrationConfig(): BtcRegistrationConfig {
        return object : BtcRegistrationConfig {
            override val notaryIrohaAccount: String
                get() = accountHelper.notaryAccount
            override val port: Int
                get() = btcRegistrationConfig.port
            override val iroha: IrohaConfig
                get() = createIrohaConfig()
            override val btcWalletPath: String
                get() = btcRegistrationConfig.btcWalletPath
        }
    }

    /** Test configuration of Notary with runtime dependencies */
    fun createEthNotaryConfig(): EthNotaryConfig {
        return object : EthNotaryConfig {
            override val registrationServiceIrohaAccount = accountHelper.registrationAccount
            override val tokenStorageAccount = accountHelper.tokenStorageAccount
            override val whitelistSetter = testConfig.whitelistSetter
            override val refund = createRefundConfig()
            override val iroha = createIrohaConfig()
            override val ethereum = ethNotaryConfig.ethereum
        }
    }

    /** Test configuration of Withdrawal service with runtime dependencies */
    fun createWithdrawalConfig(): WithdrawalServiceConfig {
        return object : WithdrawalServiceConfig {
            override val notaryIrohaAccount = accountHelper.notaryAccount
            override val tokenStorageAccount = accountHelper.tokenStorageAccount
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

    fun createRelayVacuumConfig(): RelayVacuumConfig {
        return object : RelayVacuumConfig {
            override val registrationServiceIrohaAccount = accountHelper.registrationAccount

            override val tokenStorageAccount = accountHelper.tokenStorageAccount

            /** Notary Iroha account that stores relay register */
            override val notaryIrohaAccount = accountHelper.notaryAccount

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
