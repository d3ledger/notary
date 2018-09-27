package integration.eth

import integration.helper.IntegrationHelperUtil
import mu.KLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigInteger

/**
 * Integration tests for vacuum use case
 */
class VacuumIntegrationTest {

    private val integrationHelper = IntegrationHelperUtil()

    /**
     * Test US-001 Vacuum of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and few non free relay contracts with little amount of ETH are deployed
     * @when vacuum is invoked
     * @then all ETH assets from deployed relay contracts transferred to master contract
     */
    @Test
    fun testVacuum() {
        deployFewTokens()
        integrationHelper.deployRelays(2)
        integrationHelper.registerRandomRelay()
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
        vacuum.executeVacuum(integrationHelper.configHelper.createRelayVacuumConfig())
        Thread.sleep(30_000)
        val newMasterBalance = integrationHelper.getMasterEthBalance()
        Assertions.assertEquals(newMasterBalance, initialMasterBalance.add(totalRelayBalance))
        wallets.forEach { ethPublicKey ->
            Assertions.assertEquals(integrationHelper.getEthBalance(ethPublicKey), BigInteger.ZERO)
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
