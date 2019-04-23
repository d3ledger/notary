package integration.eth

import com.d3.commons.config.IrohaCredentialConfig
import com.d3.commons.config.loadEthPasswords
import com.d3.commons.sidechain.iroha.CLIENT_DOMAIN
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import com.d3.eth.provider.ETH_PRECISION
import integration.helper.EthIntegrationHelperUtil
import integration.helper.IrohaConfigHelper
import integration.registration.RegistrationServiceTestEnvironment
import kotlinx.coroutines.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration

/**
 * Integration tests with multiple notaries for deposit case.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DepositMultiIntegrationTest {
    /** Utility functions for integration tests */
    private val integrationHelper = EthIntegrationHelperUtil()

    /** Path to public key of 2nd instance of notary */
    private val pubkeyPath2 = "deploy/iroha/keys/notary1@notary.pub"

    /** Path to private key of 2nd instance of notary */
    private val privkeyPath2 = "deploy/iroha/keys/notary1@notary.priv"

    /** Ethereum assetId in Iroha */
    private val etherAssetId = "ether#ethereum"

    private val registrationTestEnvironment = RegistrationServiceTestEnvironment(integrationHelper)
    private val ethRegistrationService: Job

    init {
        // run notary
        integrationHelper.runEthDeposit()
        registrationTestEnvironment.registrationInitialization.init()
        ethRegistrationService = GlobalScope.launch {
            integrationHelper.runEthRegistrationService(integrationHelper.ethRegistrationConfig)
        }

        // create 2nd notary config
        val irohaCredential = object : IrohaCredentialConfig {
            override val pubkeyPath = pubkeyPath2
            override val privkeyPath = privkeyPath2
            override val accountId = integrationHelper.accountHelper.notaryAccount.accountId
        }

        val ethereumPasswords = loadEthPasswords("test", "/eth/ethereum_password.properties").get()
        val ethereumConfig =
            integrationHelper.configHelper.createEthereumConfig("deploy/ethereum/keys/local/notary1.key")
        val depositConfig =
            integrationHelper.configHelper.createEthDepositConfig(
                ethereumConfig = ethereumConfig,
                notaryCredential_ = irohaCredential
            )

        val keypair = ModelUtil.loadKeypair(pubkeyPath2, privkeyPath2).get()

        integrationHelper.accountHelper.addNotarySignatory(keypair)

        // run 2nd instance of notary
        integrationHelper.runEthDeposit(ethereumPasswords, depositConfig)

    }

    /** Iroha client account */
    private val clientIrohaAccount = String.getRandomString(9)
    private val clientIrohaAccountId = "$clientIrohaAccount@$CLIENT_DOMAIN"

    /** Ethereum address to transfer to */
    private val relayWallet = registerRelay()

    private fun registerRelay(): String {
        integrationHelper.deployRelays(1)
        // register client in Iroha
        val res = integrationHelper.sendRegistrationRequest(
            clientIrohaAccount,
            ModelUtil.generateKeypair().public.toHexString(),
            registrationTestEnvironment.registrationConfig.port
        )
        Assertions.assertEquals(200, res.statusCode)
        // TODO: D3-417 Web3j cannot pass an empty list of addresses to the smart contract.
        return integrationHelper.registerClientInEth(clientIrohaAccount)
    }

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    @AfterAll
    fun dropDown() {
        ethRegistrationService.cancel()
        integrationHelper.close()
    }

    /**
     * Test US-001 Deposit of ETH with multiple notaries
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least
     * 1234000000000 Wei and 2 instances of notary running
     * @when "fromAddress" transfers 0 Wei to "relayWallet" and then "fromAddress" transfers 1234000000000 Wei
     * to "relayWallet"
     * @then Associated Iroha account balance is increased on 1234000000000 Wei
     */
    @Test
    fun depositMultisig() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            Thread.currentThread().name = this::class.simpleName
            val initialAmount = integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, etherAssetId)
            val amount = BigInteger.valueOf(1_234_000_000_000)
            // send ETH
            runBlocking { delay(2000) }
            integrationHelper.purgeAndwaitOneIrohaBlock {
                integrationHelper.sendEth(amount, relayWallet)
            }

            Assertions.assertEquals(
                BigDecimal(amount, ETH_PRECISION).add(BigDecimal(initialAmount)),
                BigDecimal(integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, etherAssetId))
            )
        }
    }

    /**
     * Test US-002 Deposit of ETH token with multiple notaries
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least 51 coin
     * (51 coin) and 2 notaries running
     * @when "fromAddress" transfers 0 tokens to "relayWallet" and then "fromAddress" transfers 51 coin to "relayWallet"
     * @then Associated Iroha account balance is increased on 51 coin
     */
    @Test
    fun depositMultisigERC20() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            val (tokenInfo, tokenAddress) = integrationHelper.deployRandomERC20Token(2)
            val assetId = "${tokenInfo.name}#ethereum"
            val initialAmount = integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId)
            val amount = BigInteger.valueOf(51)

            // send ETH
            integrationHelper.purgeAndwaitOneIrohaBlock {
                integrationHelper.sendERC20Token(tokenAddress, amount, relayWallet)
            }

            Assertions.assertEquals(
                BigDecimal(amount, tokenInfo.precision).add(BigDecimal(initialAmount)),
                BigDecimal(integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId))
            )
        }
    }
}
