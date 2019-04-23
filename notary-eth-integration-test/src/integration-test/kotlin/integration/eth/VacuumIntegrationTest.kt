package integration.eth

import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import com.d3.eth.vacuum.executeVacuum
import integration.helper.EthIntegrationHelperUtil
import integration.helper.IrohaConfigHelper
import integration.registration.RegistrationServiceTestEnvironment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KLogging
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger
import java.time.Duration

/**
 * Integration tests for vacuum use case
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VacuumIntegrationTest {

    private val integrationHelper = EthIntegrationHelperUtil()
    private val registrationTestEnvironment = RegistrationServiceTestEnvironment(integrationHelper)
    private val ethRegistrationService: Job

    init {
        registrationTestEnvironment.registrationInitialization.init()
        ethRegistrationService = GlobalScope.launch {
            integrationHelper.runEthRegistrationService(integrationHelper.ethRegistrationConfig)
        }
    }

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    @AfterAll
    fun dropDown() {
        ethRegistrationService.cancel()
        integrationHelper.close()
    }

    /**
     * Test US-001 Vacuum of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and few non free relay contracts with little amount of ETH are deployed
     * @when vacuum is invoked
     * @then all ETH assets from deployed relay contracts transferred to master contract
     */
    @Test
    fun testVacuum() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            deployFewTokens()
            integrationHelper.deployRelays(2)
            val name = String.getRandomString(9)
            // register client in Iroha
            val res = integrationHelper.sendRegistrationRequest(
                name,
                ModelUtil.generateKeypair().public.toHexString(),
                registrationTestEnvironment.registrationConfig.port
            )
            Assertions.assertEquals(200, res.statusCode)
            integrationHelper.registerClientInEth(name)
            logger.info("test is ready to proceed")
            val amount = BigInteger.valueOf(2_345_678_000_000)
            var totalRelayBalance = BigInteger.ZERO
            val wallets = integrationHelper.getRegisteredEthWallets()
            wallets.forEach { ethPublicKey ->
                integrationHelper.sendEth(amount, ethPublicKey)
            }
            logger.info("done sending")
            Thread.sleep(20_000)
            wallets.forEach { ethPublicKey ->
                val balance = integrationHelper.getEthBalance(ethPublicKey)
                totalRelayBalance = totalRelayBalance.add(balance)
                logger.info("$ethPublicKey has $balance ETH")
            }
            val initialMasterBalance = integrationHelper.getMasterEthBalance()
            logger.info("initialMasterBalance $initialMasterBalance")
            executeVacuum(
                integrationHelper.configHelper.createRelayVacuumConfig()
            )
            Thread.sleep(30_000)
            val newMasterBalance = integrationHelper.getMasterEthBalance()
            Assertions.assertEquals(newMasterBalance, initialMasterBalance.add(totalRelayBalance))
            wallets.forEach { ethPublicKey ->
                Assertions.assertEquals(
                    integrationHelper.getEthBalance(ethPublicKey),
                    BigInteger.ZERO
                )
            }
        }
    }

    private fun deployFewTokens() {
        for (i in 1..3) {
            integrationHelper.deployRandomERC20Token()
        }
    }

    /**
     * Logger
     */
    private companion object : KLogging()
}
