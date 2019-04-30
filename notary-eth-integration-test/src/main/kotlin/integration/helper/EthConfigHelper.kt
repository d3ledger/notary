package integration.helper

import com.d3.commons.config.*
import com.d3.eth.deposit.EthDepositConfig
import com.d3.eth.deposit.RefundConfig
import com.d3.eth.registration.EthRegistrationConfig
import com.d3.eth.registration.relay.RelayRegistrationConfig
import com.d3.eth.token.ERC20TokenRegistrationConfig
import com.d3.eth.vacuum.RelayVacuumConfig
import com.d3.eth.withdrawal.withdrawalservice.WithdrawalServiceConfig

/**
 *Class that handles all the configuration objects.
 */
open class EthConfigHelper(
    private val accountHelper: IrohaAccountHelper,
    open val relayRegistryContractAddress: String,
    open val masterContractAddress: String,
    open val relayImplementaionContractAddress: String
) : IrohaConfigHelper() {

    /** Ethereum password configs */
    val ethPasswordConfig = loadEthPasswords("test", "/eth/ethereum_password.properties").get()

    /** Configuration for deposit instance */
    private val ethDepositConfig by lazy {
        loadConfigs(
            "eth-deposit",
            EthDepositConfig::class.java,
            "/eth/deposit.properties"
        ).get()
    }

    fun getTestCredentialConfig(): IrohaCredentialConfig {
        return testConfig.testCredentialConfig
    }

    /** Creates config for ERC20 tokens registration */
    fun createERC20TokenRegistrationConfig(
        ethTokensFilePath_: String,
        irohaTokensFilePath_: String
    ): ERC20TokenRegistrationConfig {
        val ethTokenRegistrationConfig = loadConfigs(
            "token-registration",
            ERC20TokenRegistrationConfig::class.java,
            "/eth/token_registration.properties"
        ).get()

        return object : ERC20TokenRegistrationConfig {
            override val irohaCredential = ethTokenRegistrationConfig.irohaCredential
            override val iroha = createIrohaConfig()
            override val ethAnchoredTokensFilePath = ethTokensFilePath_
            override val irohaAnchoredTokensFilePath = irohaTokensFilePath_
            override val ethAnchoredTokenStorageAccount =
                accountHelper.ethAnchoredTokenStorageAccount.accountId
            override val irohaAnchoredTokenStorageAccount =
                accountHelper.irohaAnchoredTokenStorageAccount.accountId
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
            override val ethRelayImplementationAddress = relayImplementaionContractAddress
            override val notaryIrohaAccount = accountHelper.notaryAccount.accountId
            override val iroha = createIrohaConfig()
            override val ethereum = relayRegistrationConfig.ethereum
            override val relayRegistrationCredential =
                relayRegistrationConfig.relayRegistrationCredential
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

    /** Test configuration of Deposit with runtime dependencies */
    fun createEthDepositConfig(
        irohaConfig: IrohaConfig = createIrohaConfig(),
        ethereumConfig: EthereumConfig = object : EthereumConfig {
            override val url = ethDepositConfig.ethereum.url
            override val credentialsPath = testConfig.ethereum.credentialsPath
            override val gasPrice = ethDepositConfig.ethereum.gasPrice
            override val gasLimit = ethDepositConfig.ethereum.gasLimit
            override val confirmationPeriod = ethDepositConfig.ethereum.confirmationPeriod
        },
        notaryCredential_: IrohaCredentialConfig = accountHelper.createCredentialConfig(
            accountHelper.notaryAccount
        )
    ): EthDepositConfig {
        return object : EthDepositConfig {
            override val registrationServiceIrohaAccount =
                accountHelper.registrationAccount.accountId
            override val ethAnchoredTokenStorageAccount =
                accountHelper.ethAnchoredTokenStorageAccount.accountId
            override val ethAnchoredTokenSetterAccount = accountHelper.tokenSetterAccount.accountId
            override val irohaAnchoredTokenStorageAccount =
                accountHelper.irohaAnchoredTokenStorageAccount.accountId
            override val irohaAnchoredTokenSetterAccount =
                accountHelper.tokenSetterAccount.accountId
            override val notaryCredential = notaryCredential_
            override val refund = createRefundConfig()
            override val iroha = irohaConfig
            override val ethereum = ethereumConfig
            override val withdrawalAccountId = ethDepositConfig.withdrawalAccountId
        }
    }

    /** Test configuration of Withdrawal service with runtime dependencies */
    fun createWithdrawalConfig(
        testName: String,
        useValidEthereum: Boolean = true
    ): WithdrawalServiceConfig {
        val withdrawalConfig =
            loadConfigs(
                "withdrawal",
                WithdrawalServiceConfig::class.java,
                "/eth/withdrawal.properties"
            ).get()

        val ethereumConfig =
            if (useValidEthereum) withdrawalConfig.ethereum else getBrokenEthereumConfig(
                withdrawalConfig
            )

        return object : WithdrawalServiceConfig {
            override val notaryIrohaAccount = accountHelper.notaryAccount.accountId
            override val ethAnchoredTokenStorageAccount =
                accountHelper.ethAnchoredTokenStorageAccount.accountId
            override val ethAnchoredTokenSetterAccount = accountHelper.tokenSetterAccount.accountId
            override val irohaAnchoredTokenStorageAccount =
                accountHelper.irohaAnchoredTokenStorageAccount.accountId
            override val irohaAnchoredTokenSetterAccount =
                accountHelper.tokenSetterAccount.accountId
            override val notaryListStorageAccount = accountHelper.notaryListStorageAccount.accountId
            override val notaryListSetterAccount = accountHelper.notaryListSetterAccount.accountId
            override val registrationIrohaAccount = accountHelper.registrationAccount.accountId
            override val withdrawalCredential = withdrawalConfig.withdrawalCredential
            override val port = portCounter.incrementAndGet()
            override val iroha = createIrohaConfig()
            override val ethereum = ethereumConfig
            override val ethIrohaWithdrawalQueue = testName
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
            loadConfigs(
                "relay-vacuum",
                RelayVacuumConfig::class.java,
                "/eth/vacuum.properties"
            ).get()
        return object : RelayVacuumConfig {
            override val registrationServiceIrohaAccount =
                accountHelper.registrationAccount.accountId

            override val ethAnchoredTokenStorageAccount =
                accountHelper.ethAnchoredTokenStorageAccount.accountId
            override val ethAnchoredTokenSetterAccount = accountHelper.tokenSetterAccount.accountId
            override val irohaAnchoredTokenStorageAccount =
                accountHelper.irohaAnchoredTokenStorageAccount.accountId
            override val irohaAnchoredTokenSetterAccount =
                accountHelper.tokenSetterAccount.accountId
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
    fun createEthereumConfig(credentialsPath: String = ethDepositConfig.ethereum.credentialsPath): EthereumConfig {
        return object : EthereumConfig {
            override val confirmationPeriod = ethDepositConfig.ethereum.confirmationPeriod
            override val credentialsPath = credentialsPath
            override val gasLimit = ethDepositConfig.ethereum.gasLimit
            override val gasPrice = ethDepositConfig.ethereum.gasPrice
            override val url = ethDepositConfig.ethereum.url
        }
    }

}
