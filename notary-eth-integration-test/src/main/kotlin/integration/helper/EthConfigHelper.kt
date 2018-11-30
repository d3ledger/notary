package integration.helper

import config.*
import notary.eth.EthNotaryConfig
import notary.eth.RefundConfig
import registration.eth.EthRegistrationConfig
import registration.eth.relay.RelayRegistrationConfig
import token.ERC20TokenRegistrationConfig
import vacuum.RelayVacuumConfig
import withdrawalservice.WithdrawalServiceConfig
import java.util.concurrent.atomic.AtomicInteger

/**
 *Class that handles all the configuration objects.
 */
open class EthConfigHelper(
    private val accountHelper: IrohaAccountHelper,
    open val relayRegistryContractAddress: String,
    open val masterContractAddress: String
) : IrohaConfigHelper() {

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

    /** Creates config for ETH relays registration */
    fun createRelayRegistrationConfig(): RelayRegistrationConfig {
        val relayRegistrationConfig =
            loadConfigs("relay-registration", RelayRegistrationConfig::class.java, "/eth/relay_registration.properties")

        return object : RelayRegistrationConfig {
            override val number = relayRegistrationConfig.number
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
            loadConfigs("relay-vacuum", RelayVacuumConfig::class.java, "/eth/vacuum.properties")
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

    companion object {
        /** Port counter, so new port is generated for each run */
        private val portCounter = AtomicInteger(19_999)
    }
}
