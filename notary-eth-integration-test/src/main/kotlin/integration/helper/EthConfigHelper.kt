package integration.helper

import config.*
import notary.eth.EthNotaryConfig
import notary.eth.RefundConfig
import registration.eth.EthRegistrationConfig
import registration.eth.relay.RelayRegistrationConfig
import token.ERC20TokenRegistrationConfig
import vacuum.RelayVacuumConfig
import withdrawalservice.WithdrawalServiceConfig

/**
 *Class that handles all the configuration objects.
 */
open class EthConfigHelper(
    private val accountHelper: IrohaAccountHelper,
    open val relayRegistryContractAddress: String,
    open val masterContractAddress: String
) : IrohaConfigHelper() {

    /** Ethereum password configs */
    val ethPasswordConfig = loadEthPasswords("test", "/eth/ethereum_password.properties").get()

    /** Configuration for notary instance */
    private val ethNotaryConfig by lazy {
        loadConfigs(
            "eth-notary",
            EthNotaryConfig::class.java,
            "/eth/notary.properties"
        ).get()
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
        ).get()

        return object : ERC20TokenRegistrationConfig {
            override val irohaCredential = ethTokenRegistrationConfig.irohaCredential
            override val iroha = createIrohaConfig()
            override val tokensFilePath = tokensFilePath_
            override val tokenStorageAccount = accountHelper.notaryAccount.accountId
            override val xorEthereumAddress = "0x0000000000000000000000000000000000000000"
        }
    }

    /** Creates config for ETH relays registration */
    fun createRelayRegistrationConfig(): RelayRegistrationConfig {
        val relayRegistrationConfig =
            loadConfigs(
                "relay-registration",
                RelayRegistrationConfig::class.java,
                "/eth/relay_registration.properties"
            ).get()

        return object : RelayRegistrationConfig {
            override val number = relayRegistrationConfig.number
            override val replenishmentPeriod = relayRegistrationConfig.replenishmentPeriod
            override val ethMasterWallet = masterContractAddress
            override val notaryIrohaAccount = accountHelper.notaryAccount.accountId
            override val iroha = createIrohaConfig()
            override val ethereum = relayRegistrationConfig.ethereum
            override val relayRegistrationCredential = relayRegistrationConfig.relayRegistrationCredential
        }
    }

    /**
     * Test configuration for refund endpoint
     * Create unique port for refund for every call
     */
    fun createRefundConfig(): RefundConfig {
        return object : RefundConfig {
            override val port = portCounter.incrementAndGet()
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
            override val whitelistSetter = accountHelper.registrationAccount.accountId
            override val notaryCredential = notaryCredential_
            override val refund = createRefundConfig()
            override val iroha = irohaConfig
            override val ethereum = ethereumConfig
            override val withdrawalAccountId = ethNotaryConfig.withdrawalAccountId
        }
    }

    /** Test configuration of Withdrawal service with runtime dependencies */
    fun createWithdrawalConfig(useValidEthereum: Boolean = true): WithdrawalServiceConfig {
        val withdrawalConfig =
            loadConfigs("withdrawal", WithdrawalServiceConfig::class.java, "/eth/withdrawal.properties").get()

        val ethereumConfig =
            if (useValidEthereum) withdrawalConfig.ethereum else getBrokenEthereumConfig(withdrawalConfig)

        return object : WithdrawalServiceConfig {
            override val notaryIrohaAccount = accountHelper.notaryAccount.accountId
            override val tokenStorageAccount = accountHelper.tokenStorageAccount.accountId
            override val tokenSetterAccount = accountHelper.tokenSetterAccount.accountId
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount.accountId
            override val notaryListSetterAccount = accountHelper.notaryListSetterAccount.accountId
            override val registrationIrohaAccount = accountHelper.registrationAccount.accountId
            override val withdrawalCredential = withdrawalConfig.withdrawalCredential
            override val port = portCounter.incrementAndGet()
            override val iroha = createIrohaConfig()
            override val ethereum = ethereumConfig
            override val ethIrohaWithdrawalQueue = withdrawalConfig.ethIrohaWithdrawalQueue
        }
    }

    fun getBrokenEthereumConfig(withdrawalServiceConfig: WithdrawalServiceConfig): EthereumConfig {
        return object : EthereumConfig {
            override val url = withdrawalServiceConfig.ethereum.url
            override val credentialsPath = withdrawalServiceConfig.ethereum.credentialsPath
            override val gasPrice = 0L
            override val gasLimit = 0L
            override val confirmationPeriod = 0L
        }
    }

    /** Test configuration of Registration with runtime dependencies */
    fun createEthRegistrationConfig(ethereumConfig: EthereumConfig): EthRegistrationConfig {
        return object : EthRegistrationConfig {
            override val ethRelayRegistryAddress = relayRegistryContractAddress
            override val ethereum = ethereumConfig
            override val port = portCounter.incrementAndGet()
            override val relayRegistrationIrohaAccount = accountHelper.registrationAccount.accountId
            override val notaryIrohaAccount = accountHelper.notaryAccount.accountId
            override val iroha = createIrohaConfig()
            override val registrationCredential =
                accountHelper.createCredentialConfig(accountHelper.registrationAccount)
        }
    }

    fun createRelayVacuumConfig(): RelayVacuumConfig {
        val vacuumConfig =
            loadConfigs("relay-vacuum", RelayVacuumConfig::class.java, "/eth/vacuum.properties").get()
        return object : RelayVacuumConfig {
            override val registrationServiceIrohaAccount = accountHelper.registrationAccount.accountId
            override val tokenStorageAccount = accountHelper.tokenStorageAccount.accountId

            override val tokenSetterAccount = accountHelper.tokenSetterAccount.accountId

            /** Notary Iroha account that stores relay register */
            override val notaryIrohaAccount = accountHelper.notaryAccount.accountId

            override val vacuumCredential = getTestCredentialConfig()
            /** Iroha configurations */
            override val iroha = createIrohaConfig()

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

}
