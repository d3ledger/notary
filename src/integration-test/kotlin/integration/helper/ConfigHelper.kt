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

class ConfigHelper {

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

    /** Test configuration for Iroha */
    fun createIrohaConfig(creator: String = testConfig.iroha.creator): IrohaConfig {
        return object : IrohaConfig {
            override val hostname: String
                get() = testConfig.iroha.hostname
            override val port: Int
                get() = testConfig.iroha.port
            override val creator = creator
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

    fun createBtcNotaryConfig(registrationAccount: String): BtcNotaryConfig {
        return object : BtcNotaryConfig {
            override val notaryIrohaAccount: String
                get() = btcNotaryConfig.notaryIrohaAccount
            override val iroha: IrohaConfig
                get() = createIrohaConfig(registrationAccount)
            override val bitcoin: BitcoinConfig
                get() = btcNotaryConfig.bitcoin
        }
    }

    fun createBtcRegistrationConfig(registrationAccount: String): BtcRegistrationConfig {
        return object : BtcRegistrationConfig {
            override val port: Int
                get() = btcRegistrationConfig.port
            override val notaryIrohaAccount: String
                get() = btcRegistrationConfig.notaryIrohaAccount
            override val iroha: IrohaConfig
                get() = createIrohaConfig(registrationAccount)
            override val btcWalletPath: String
                get() = btcRegistrationConfig.btcWalletPath
        }
    }

    /** Test configuration of Notary with runtime dependencies */
    fun createEthNotaryConfig(registrationAccount: String, tokenStorageAccount: String): EthNotaryConfig {
        return object : EthNotaryConfig {
            override val registrationServiceIrohaAccount = registrationAccount
            override val tokenStorageAccount = tokenStorageAccount
            override val whitelistSetter = testConfig.whitelistSetter
            override val refund = createRefundConfig()
            override val iroha = ethNotaryConfig.iroha
            override val ethereum = ethNotaryConfig.ethereum
        }
    }

    /** Test configuration of Withdrawal service with runtime dependencies */
    fun createWithdrawalConfig(
        registrationAccount: String,
        newTokenStorageAccount: String,
        newNotaryListStorageAccount: String,
        newNotaryListSetterAccount: String
    ): WithdrawalServiceConfig {
        return object : WithdrawalServiceConfig {
            override val notaryIrohaAccount = withdrawalConfig.notaryIrohaAccount
            override val tokenStorageAccount = newTokenStorageAccount
            override val notaryListStorageAccount = newNotaryListStorageAccount
            override val notaryListSetterAccount = newNotaryListSetterAccount
            override val registrationIrohaAccount = registrationAccount
            override val iroha = withdrawalConfig.iroha
            override val ethereum = withdrawalConfig.ethereum
        }
    }

    /** Test configuration of Registration with runtime dependencies */
    fun createEthRegistrationConfig(registrationAccount: String): EthRegistrationConfig {
        return object : EthRegistrationConfig {
            override val port = portCounter.incrementAndGet()
            override val relayRegistrationIrohaAccount = registrationAccount
            override val notaryIrohaAccount = testConfig.notaryIrohaAccount
            override val iroha = createIrohaConfig(creator = registrationAccount)
        }
    }

    fun createRelayVacuumConfig(registrationAccount: String, tokenStorageAccount: String): RelayVacuumConfig {
        return object : RelayVacuumConfig {
            override val registrationServiceIrohaAccount = registrationAccount

            override val tokenStorageAccount = tokenStorageAccount

            /** Notary Iroha account that stores relay register */
            override val notaryIrohaAccount = testConfig.notaryIrohaAccount

            /** Iroha configurations */
            override val iroha = testConfig.iroha

            /** Ethereum configurations */
            override val ethereum = testConfig.ethereum
        }
    }

    companion object {

        /** Port counter, so new port is generated for each run */
        private val portCounter = AtomicInteger(19_999)
    }
}
