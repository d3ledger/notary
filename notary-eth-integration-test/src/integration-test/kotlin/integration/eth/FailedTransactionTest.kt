package integration.eth

import com.d3.commons.sidechain.iroha.CLIENT_DOMAIN
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import com.d3.eth.provider.ETH_DOMAIN
import com.d3.eth.token.EthTokenInfo
import integration.helper.EthIntegrationHelperUtil
import integration.helper.IrohaConfigHelper
import integration.registration.RegistrationServiceTestEnvironment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.protocol.exceptions.TransactionException
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FailedTransactionTest {
    val integrationHelper = EthIntegrationHelperUtil()

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    private val registrationTestEnvironment = RegistrationServiceTestEnvironment(integrationHelper)
    private val ethRegistrationService: Job

    init {
        integrationHelper.runEthDeposit()
        registrationTestEnvironment.registrationInitialization.init()
        ethRegistrationService = GlobalScope.launch {
            integrationHelper.runEthRegistrationService(integrationHelper.ethRegistrationConfig)
        }
    }

    @AfterAll
    fun dropDown() {
        ethRegistrationService.cancel()
        integrationHelper.close()
    }

    /**
     * Reverted Ether transfer transaction test
     * @given Ethereum node, Iroha and notary are running, failer contract is deployed and registered as relay,
     * new user registers in Iroha
     * @when send Ether to relay account
     * @then user account in Iroha has 0 balance of Ether
     */
    @Test
    fun failedEtherTransferTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            val failerAddress = integrationHelper.deployFailer()
            integrationHelper.registerRelayByAddress(failerAddress)
            val clientAccount = String.getRandomString(9)
            // register client in Iroha
            val res = integrationHelper.sendRegistrationRequest(
                clientAccount,
                ModelUtil.generateKeypair().public.toHexString(),
                registrationTestEnvironment.registrationConfig.port
            )
            Assertions.assertEquals(200, res.statusCode)
            integrationHelper.registerClientWithoutRelay(clientAccount)
            integrationHelper.sendEth(BigInteger.valueOf(1), failerAddress)
            integrationHelper.waitOneEtherBlock()
            assertEquals(BigInteger.ZERO, integrationHelper.getEthBalance(failerAddress))
            val irohaBalance =
                integrationHelper.getIrohaAccountBalance("$clientAccount@$CLIENT_DOMAIN", "ether#ethereum")
            assertEquals(BigDecimal.ZERO, BigDecimal(irohaBalance))
        }
    }

    /**
     * Reverted Ether transfer transaction test
     * @given Ethereum node, Iroha and notary are running, failer contract is deployed twice,
     * one of them is registered as relay, another one is registered as ERC-20 token and
     * new user registers in Iroha
     * @when send tokens to relay account
     * @then user account in Iroha has 0 balance of ERC-20 token
     */
    @Test
    fun failedTokenTransferTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            val failerAddress = integrationHelper.deployFailer()
            val anotherFailerAddress = integrationHelper.deployFailer()
            integrationHelper.registerRelayByAddress(failerAddress)
            val clientAccount = String.getRandomString(9)
            // register client in Iroha
            val res = integrationHelper.sendRegistrationRequest(
                clientAccount,
                ModelUtil.generateKeypair().public.toHexString(),
                registrationTestEnvironment.registrationConfig.port
            )
            Assertions.assertEquals(200, res.statusCode)
            integrationHelper.registerClientWithoutRelay(clientAccount)
            val coinName = String.getRandomString(9)
            integrationHelper.addEthAnchoredERC20Token(
                anotherFailerAddress,
                EthTokenInfo(coinName, ETH_DOMAIN, 0)
            )

            // web3j throws exception in case of contract function call revert
            // so let's catch and ignore it
            try {
                integrationHelper.sendERC20Token(anotherFailerAddress, BigInteger.valueOf(1), failerAddress)
            } catch (e: TransactionException) {
            }

            integrationHelper.waitOneEtherBlock()

            // actually this test passes even without transaction status check
            // it's probably impossible to get some money deposit to iroha
            // because logs are empty for reverted transactions
            // but let's leave it for a rainy day
            val irohaBalance =
                integrationHelper.getIrohaAccountBalance("$clientAccount@$CLIENT_DOMAIN", "$coinName#ethereum")
            assertEquals(BigDecimal.ZERO, BigDecimal(irohaBalance))
        }
    }
}
