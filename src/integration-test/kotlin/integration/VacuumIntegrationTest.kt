package integration

import com.github.kittinunf.result.failure
import config.EthereumPasswords
import config.TestConfig
import config.loadConfigs
import kotlinx.coroutines.experimental.async
import mu.KLogging
import notary.EthWalletsProviderIrohaImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.web3j.protocol.core.DefaultBlockParameterName
import registration.relay.RelayRegistrationConfig
import sidechain.eth.util.DeployHelper
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import util.getRandomString
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
    private val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties")

    private val relayRegistrationConfig = loadConfigs("relay-registration", RelayRegistrationConfig::class.java)

    /** Ethereum password configs */
    private val passwordConfig = loadConfigs("test", EthereumPasswords::class.java, "/ethereum_password.properties")

    /** Registration service port */
    private val registrationServicePort = 8083

    /** Ethereum utils */
    private val deployHelper = DeployHelper(testConfig.ethereum, passwordConfig)

    /** Iroha keypair */
    private val keypair = ModelUtil.loadKeypair(testConfig.iroha.pubkeyPath, testConfig.iroha.privkeyPath).get()

    /** Iroha network */
    private val irohaNetwork = IrohaNetworkImpl(testConfig.iroha.hostname, testConfig.iroha.port)

    private val ethWalletsProvider = EthWalletsProviderIrohaImpl(
        testConfig.iroha, keypair, irohaNetwork, testConfig.notaryIrohaAccount, testConfig.registrationIrohaAccount
    )

    /**
     * Test US-001 Vacuum of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and few non free relay contracts with little amount of ETH are deployed
     * @when vacuum is invoked
     * @then all ETH from deployed relay contracts goes to master contract
     */
    @Test
    fun testVacuum() {
        val name = String.getRandomString(9)
        val amount = BigInteger.valueOf(2_345_678_000_000)
        var totalRelayBalance = BigInteger.ZERO
        async {
            registration.main(emptyArray())
        }
        Thread.sleep(3_000)
        val res = khttp.post(
            "http://127.0.0.1:$registrationServicePort/users",
            data = mapOf("name" to name, "pubkey" to keypair.publicKey().hex())
        )
        Assertions.assertEquals(200, res.statusCode)
        logger.info("user was registered")
        val wallets = ethWalletsProvider.getWallets().get().keys
        wallets.forEach { ethPublicKey ->
            deployHelper.sendEthereum(amount, ethPublicKey)
        }
        logger.info("done sending")
        Thread.sleep(180_000)
        wallets.forEach { ethPublicKey ->
            val balance = getEthBalance(ethPublicKey)
            totalRelayBalance = totalRelayBalance.add(balance)
            logger.info("$ethPublicKey has $balance ETH")
        }
        val initialMasterBalance = getEthBalance(relayRegistrationConfig.ethMasterWallet)
        logger.info("initialMasterBalance $initialMasterBalance")
        async {
            vacuum.main(emptyArray())
        }
        Thread.sleep(600_000)
        val newMasterBalance = getEthBalance(relayRegistrationConfig.ethMasterWallet)
        Assertions.assertEquals(newMasterBalance, initialMasterBalance.add(totalRelayBalance))
        wallets.forEach { ethPublicKey ->
            Assertions.assertEquals(getEthBalance(ethPublicKey), BigInteger.ZERO)
        }
    }

    /**
     * Return ETH balance for a given address
     */
    private fun getEthBalance(address: String): BigInteger {
        return deployHelper.web3.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().balance
    }

    /**
     * Logger
     */
    private companion object : KLogging()
}
