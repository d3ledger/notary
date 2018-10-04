package integration.helper

import config.*
import integration.TestConfig
import notary.btc.config.BtcNotaryConfig
import notary.eth.EthNotaryConfig
import notary.eth.RefundConfig
import pregeneration.btc.config.BtcPreGenConfig
import registration.btc.BtcRegistrationConfig
import registration.eth.EthRegistrationConfig
import registration.eth.relay.RelayRegistrationConfig
import token.ERC20TokenRegistrationConfig
import vacuum.RelayVacuumConfig
import withdrawalservice.WithdrawalServiceConfig
import java.util.concurrent.atomic.AtomicInteger

/**
 * Class that handles all the configuration objects.
 */
class ConfigHelper(
    private val accountHelper: AccountHelper,
    val relayRegistryContractAddress: String
) {

    /** Configurations for tests */
    val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties")

    /** Ethereum password configs */
    val ethPasswordConfig = loadEthPasswords("test", "/eth/ethereum_password.properties")

    /** Configuration for notary instance */
    private val ethNotaryConfig by lazy {
        loadConfigs(
            "eth-notary",
            EthNotaryConfig::class.java,
            "/eth/notary.properties"
        )
    }

    fun getTestCredentialConfig(): IrohaCredentialConfig {
        return testConfig.testCredentialConfig
    }

    /** Creates config for ERC20 tokens registration */
    fun createERC20TokenRegistrationConfig(tokensFilePath_: String): ERC20TokenRegistrationConfig {
        val ethTokenRegistrationConfig = loadConfigs(
            "token-registration",
            ERC20TokenRegistrationConfig::class.java,
            "/eth/token_registration.properties"
        )

        return object : ERC20TokenRegistrationConfig {
            override val irohaCredential = ethTokenRegistrationConfig.irohaCredential
            override val iroha = createIrohaConfig()
            override val tokensFilePath = tokensFilePath_
            override val tokenStorageAccount = accountHelper.notaryAccount.accountId
        }
    }

    /** Creates config for BTC multisig addresses generation */
    fun createBtcPreGenConfig(): BtcPreGenConfig {
        val btcPkPreGenConfig =
            loadConfigs("btc-pregen", BtcPreGenConfig::class.java, "/btc/pregeneration.properties")

        return object : BtcPreGenConfig {
            override val healthCheckPort = btcPkPreGenConfig.healthCheckPort
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount.accountId
            override val notaryListSetterAccount = accountHelper.notaryListSetterAccount.accountId
            override val mstRegistrationAccount =
                accountHelper.createCredentialConfig(accountHelper.mstRegistrationAccount)
            override val pubKeyTriggerAccount = btcPkPreGenConfig.pubKeyTriggerAccount
            override val notaryAccount = accountHelper.notaryAccount.accountId
            override val iroha = createIrohaConfig()
            override val btcWalletFilePath = btcPkPreGenConfig.btcWalletFilePath
            override val registrationAccount = accountHelper.createCredentialConfig(accountHelper.registrationAccount)
        }
    }

    /** Creates config for ETH relays registration */
    fun createRelayRegistrationConfig(): RelayRegistrationConfig {
        val relayRegistrationConfig =
            loadConfigs("relay-registration", RelayRegistrationConfig::class.java, "/eth/relay_registration.properties")

        return object : RelayRegistrationConfig {
            override val number = relayRegistrationConfig.number
            override val ethMasterWallet = relayRegistrationConfig.ethMasterWallet
            override val notaryIrohaAccount = accountHelper.notaryAccount.accountId
            override val iroha = createIrohaConfig()
            override val ethereum = relayRegistrationConfig.ethereum
            override val relayRegistrationCredential = relayRegistrationConfig.relayRegistrationCredential
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
        val btcNotaryConfig = loadConfigs("btc-notary", BtcNotaryConfig::class.java, "/btc/notary.properties")

        return object : BtcNotaryConfig {
            override val healthCheckPort = btcNotaryConfig.healthCheckPort
            override val registrationAccount = accountHelper.registrationAccount.accountId
            override val iroha = createIrohaConfig()
            override val bitcoin = btcNotaryConfig.bitcoin
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount.accountId
            override val notaryListSetterAccount = accountHelper.notaryListSetterAccount.accountId
            override val notaryCredential = accountHelper.createCredentialConfig(accountHelper.notaryAccount)
        }
    }

    fun createBtcRegistrationConfig(): BtcRegistrationConfig {
        val btcRegistrationConfig =
            loadConfigs("btc-registration", BtcRegistrationConfig::class.java, "/btc/registration.properties")

        return object : BtcRegistrationConfig {
            override val mstRegistrationAccount = accountHelper.mstRegistrationAccount.accountId
            override val port = btcRegistrationConfig.port
            override val registrationCredential = btcRegistrationConfig.registrationCredential
            override val iroha = createIrohaConfig()
            override val btcWalletPath = btcRegistrationConfig.btcWalletPath
        }
    }

    /** Test configuration of Notary with runtime dependencies */
    fun createEthNotaryConfig(
        irohaConfig: IrohaConfig = createIrohaConfig(),
        ethereumConfig: EthereumConfig = ethNotaryConfig.ethereum,
        notaryCredential_: IrohaCredentialConfig = accountHelper.createCredentialConfig(
            accountHelper.notaryAccount
        )
    ): EthNotaryConfig {
        return object : EthNotaryConfig {
            override val registrationServiceIrohaAccount = accountHelper.registrationAccount.accountId
            override val tokenStorageAccount = accountHelper.tokenStorageAccount.accountId
            override val tokenSetterAccount = accountHelper.tokenSetterAccount.accountId
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount.accountId
            override val notaryListSetterAccount = accountHelper.notaryListSetterAccount.accountId
            override val whitelistSetter = accountHelper.whitelistSetter.accountId
            override val notaryCredential = notaryCredential_
            override val refund = createRefundConfig()
            override val iroha = irohaConfig
            override val ethereum = ethereumConfig
        }
    }

    /** Test configuration of Withdrawal service with runtime dependencies */
    fun createWithdrawalConfig(): WithdrawalServiceConfig {
        val withdrawalConfig =
            loadConfigs("withdrawal", WithdrawalServiceConfig::class.java, "/eth/withdrawal.properties")

        return object : WithdrawalServiceConfig {
            override val notaryIrohaAccount = accountHelper.notaryAccount.accountId
            override val tokenStorageAccount = accountHelper.tokenStorageAccount.accountId
            override val tokenSetterAccount = accountHelper.tokenSetterAccount.accountId
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount.accountId
            override val notaryListSetterAccount = accountHelper.notaryListSetterAccount.accountId
            override val registrationIrohaAccount = accountHelper.registrationAccount.accountId
            override val withdrawalCredential = withdrawalConfig.withdrawalCredential
            override val iroha = createIrohaConfig()
            override val ethereum = withdrawalConfig.ethereum
        }
    }

    /** Test configuration of Registration with runtime dependencies */
    fun createEthRegistrationConfig(): EthRegistrationConfig {
        val ethRegistrationConfig =
            loadConfigs("eth-registration", EthRegistrationConfig::class.java, "/eth/registration.properties")

        return object : EthRegistrationConfig {
            override val ethRelayRegistryAddress = relayRegistryContractAddress
            override val ethereum = ethRegistrationConfig.ethereum
            override val port = portCounter.incrementAndGet()
            override val relayRegistrationIrohaAccount = accountHelper.registrationAccount.accountId
            override val notaryIrohaAccount = accountHelper.notaryAccount.accountId
            override val iroha = createIrohaConfig()
            override val registrationCredential =
                accountHelper.createCredentialConfig(accountHelper.registrationAccount)
        }
    }

    fun createRelayVacuumConfig(): RelayVacuumConfig {
        return object : RelayVacuumConfig {
            override val registrationServiceIrohaAccount = accountHelper.registrationAccount.accountId
            override val tokenStorageAccount = accountHelper.tokenStorageAccount.accountId

            /** Notary Iroha account that stores relay register */
            override val notaryIrohaAccount = accountHelper.notaryAccount.accountId

            override val vacuumCredential = getTestCredentialConfig()
            override val iroha = createIrohaConfig()
            override val ethereum = testConfig.ethereum
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
