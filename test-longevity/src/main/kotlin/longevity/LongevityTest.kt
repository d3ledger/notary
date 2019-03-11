package longevity

import com.github.kittinunf.result.Result
import com.d3.commons.config.IrohaCredentialConfig
import com.d3.commons.config.loadEthPasswords
import integration.helper.EthIntegrationHelperUtil
import integration.helper.NotaryClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KLogging
import com.d3.eth.provider.ETH_PRECISION
import com.d3.commons.sidechain.iroha.util.ModelUtil
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Class for longevity testing.
 * It is supposed to be used for long-running tests.
 */
class LongevityTest {
    private val integrationHelper = EthIntegrationHelperUtil()

    private val masterContract = integrationHelper.masterContract.contractAddress

    /**
     * Number of clients for tests.
     * Since clients keys for ethereum and iroha are files [totalClients] should be in [1..5]
     */
    private val totalClients = 1

    /** Create d3 clients */
    private val clients = (0..totalClients - 1).map { clientNumber ->
        NotaryClient(
            integrationHelper,
            integrationHelper.configHelper.createEthereumConfig("deploy/ethereum/keys/local/client$clientNumber.key"),
            loadEthPasswords("client$clientNumber", "/eth/ethereum_password.properties").get()
        )
    }

    /** Run 4 instances of notary */
    private fun runNotaries() {
        // run first instance with default configs
        integrationHelper.runEthNotary()

        // launch the rest
        (1..3).forEach {
            val irohaCredential = object : IrohaCredentialConfig {
                override val pubkeyPath = "deploy/iroha/keys/notary$it@notary.pub"
                override val privkeyPath = "deploy/iroha/keys/notary$it@notary.priv"
                override val accountId = integrationHelper.accountHelper.notaryAccount.accountId
            }

            val ethereumPasswords = loadEthPasswords("notary$it", "/eth/ethereum_password.properties").get()

            val ethereumConfig =
                integrationHelper.configHelper.createEthereumConfig("deploy/ethereum/keys/local/notary$it.key")

            val notaryConfig =
                integrationHelper.configHelper.createEthNotaryConfig(
                    ethereumConfig = ethereumConfig,
                    notaryCredential_ = irohaCredential
                )

            integrationHelper.accountHelper.addNotarySignatory(
                ModelUtil.loadKeypair(
                    irohaCredential.pubkeyPath,
                    irohaCredential.privkeyPath
                ).get()
            )

            integrationHelper.runEthNotary(ethereumPasswords, notaryConfig)
        }
    }

    /**
     * Run all notary services.
     */
    private fun runServices() {
        runNotaries()
        GlobalScope.launch { integrationHelper.runRegistrationService() }
        GlobalScope.launch { integrationHelper.runEthWithdrawalService() }

        // wait until services are up
        Thread.sleep(10_000)
    }

    /**
     * Register all clients.
     */
    private fun registerClients(): Result<Unit, Exception> {
        integrationHelper.deployRelays(5)
        return Result.of {
            for (client in clients) {
                val status = client.signUp().statusCode
                if (status != 200)
                    throw Exception("Cannot register client ${client.accountId}, status code: $status")
            }
        }
    }

    /**
     * Run test strategy.
     */
    private fun runTest() {
        logger.info { "client count ${clients.size}" }

        clients.forEach { client ->
            GlobalScope.launch { delay(3_000) }
            GlobalScope.launch {
                logger.info { "start client ${client.accountId}" }
                while (true) {
                    val amount = BigInteger.valueOf(12_000_000_000)
                    logger.info { "Client ${client.name} perform deposit of $amount wei" }
                    client.deposit(amount)
                    launch { delay(20_000) }


                    val ethBalanceBefore = client.getEthBalance()
                    val irohaBalanceBefore = client.getIrohaBalance()
                    logger.info { "Master eth balance ${integrationHelper.getEthBalance(masterContract)} after deposit" }
                    logger.info { "Clietn ${client.name} eth balance: $ethBalanceBefore after deposit" }
                    logger.info { "Client ${client.name} iroha balance $irohaBalanceBefore after deposit" }

                    val decimalAmount = BigDecimal(amount, ETH_PRECISION.toInt())

                    if (!BigDecimal(irohaBalanceBefore).equals(decimalAmount))
                        logger.warn { "Client ${client.name} has wrong iroha balance. Expected ${decimalAmount.toPlainString()}, but got before: $irohaBalanceBefore" }

                    logger.info { "Client ${client.name} perform withdrawal of ${decimalAmount.toPlainString()}" }
                    client.withdraw(decimalAmount.toPlainString())
                    launch { delay(20_000) }

                    val ethBalanceAfter = client.getEthBalance()
                    val irohaBalanceAfter = client.getIrohaBalance()
                    logger.info { "Master eth balance ${integrationHelper.getEthBalance(masterContract)} after withdrawal" }
                    logger.info { "Clietn ${client.name} eth balance: $ethBalanceAfter after withdrawal" }
                    logger.info { "Client ${client.name} iroha balance $irohaBalanceAfter after withdrawal" }

                    // check balance
                    if (!ethBalanceBefore.add(amount).equals(ethBalanceAfter))
                        logger.warn { "Client ${client.name} has wrong eth balance. Expected equal but got before: $ethBalanceBefore, after: $ethBalanceAfter" }

                    if (!BigDecimal(irohaBalanceBefore).equals(BigDecimal(amount, ETH_PRECISION.toInt())))
                        logger.warn { "Client ${client.name} has wrong iroha balance. Expected 0, but got after: $irohaBalanceBefore" }

                    if (!BigDecimal(irohaBalanceAfter).equals(BigDecimal(BigInteger.ZERO, ETH_PRECISION.toInt())))
                        logger.warn { "Client ${client.name} has wrong iroha balance. Expected 0, but got after: $irohaBalanceAfter" }
                }
            }
        }
    }

    /**
     * Run all.
     */
    fun run() {
        runServices()
        registerClients()

        // send ether to master account for fee
        val toMaster = BigInteger.valueOf(150_000_000_000_000)
        integrationHelper.sendEth(toMaster, integrationHelper.masterContract.contractAddress)

        runTest()
    }

    /**
     * Logger
     */
    companion object : KLogging()

}

