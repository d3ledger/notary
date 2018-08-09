package integration

import com.github.kittinunf.result.failure
import config.*
import integration.helper.IntegrationHelperUtil
import kotlinx.coroutines.experimental.async
import mu.KLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import sidechain.iroha.IrohaInitialization
import vacuum.RelayVacuumConfig
import java.math.BigInteger

/**
 * Integration tests for vacuum use case
 */
class VacuumIntegrationTest {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure {
                println(it)
                System.exit(1)
            }
    }

    /** Configurations for tests */
    private val testConfig = loadConfigs("test", TestConfig::class.java)

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
        Thread.sleep(120_000)
        wallets.forEach { ethPublicKey ->
            val balance = integrationHelper.getEthBalance(ethPublicKey)
            totalRelayBalance = totalRelayBalance.add(balance)
            logger.info("$ethPublicKey has $balance ETH")
        }
        val initialMasterBalance = integrationHelper.getMasterEthBalance()
        logger.info("initialMasterBalance $initialMasterBalance")
        async {
            vacuum.executeVacuum(getRelayConfig())
        }
        Thread.sleep(300_000)
        val newMasterBalance = integrationHelper.getMasterEthBalance()
        Assertions.assertEquals(newMasterBalance, initialMasterBalance.add(totalRelayBalance))
        wallets.forEach { ethPublicKey ->
            Assertions.assertEquals(integrationHelper.getEthBalance(ethPublicKey), BigInteger.ZERO)
        }
    }

    private fun getRelayConfig(): RelayVacuumConfig {
        return object : RelayVacuumConfig {
            override val registrationServiceIrohaAccount = integrationHelper.masterAccount

            /** Notary Iroha account that stores relay register */
            override val notaryIrohaAccount = testConfig.notaryIrohaAccount

            /** Iroha configurations */
            override val iroha = testConfig.iroha

            /** Ethereum configurations */
            override val ethereum = testConfig.ethereum

            /** Db configurations */
            override val db = testConfig.db
        }
    }

    /**
     * Logger
     */
    private companion object : KLogging()
}
