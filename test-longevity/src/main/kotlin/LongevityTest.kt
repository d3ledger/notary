import com.github.kittinunf.result.Result
import config.IrohaCredentialConfig
import config.loadEthPasswords
import integration.helper.IntegrationHelperUtil
import kotlinx.coroutines.experimental.launch
import provider.eth.ETH_PRECISION
import sidechain.iroha.util.ModelUtil
import java.math.BigDecimal
import java.math.BigInteger

class LongevityTest {
    private val integrationHelper = IntegrationHelperUtil()

    /** create 5 d3 clients */
    val clients = (0..4).map { clientNumber ->
        NotaryClient(
            integrationHelper,
            integrationHelper.configHelper.createEthereumConfig("deploy/ethereum/keys/local/client$clientNumber.key"),
            loadEthPasswords("client$clientNumber", "/eth/ethereum_password.properties")
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

            val ethereumPasswords = loadEthPasswords("notary$it", "/eth/ethereum_password.properties")

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

        // finally lock master
        integrationHelper.lockEthMasterSmartcontract()
    }

    /**
     * Run all notary services.
     */
    fun runServices() {
        runNotaries()
        launch { integrationHelper.runRegistrationService() }
        launch { integrationHelper.runEthWithdrawalService() }

        // wait until services are up
        Thread.sleep(10_000)
    }

    fun registerClients(): Result<Unit, Exception> {
        integrationHelper.deployRelays(5)
        return Result.of {
            for (client in clients) {
                val status = client.signUp().statusCode
                if (status != 200)
                    throw Exception("Cannot register client ${client.accountId}, status code: $status")
            }
        }
    }

    fun run() {
        runServices()
        registerClients()

        // send ether to master account for fee
        val toMaster = BigInteger.valueOf(150_000_000_000_000)
        val decimalToMaster = BigDecimal(toMaster, ETH_PRECISION.toInt()).toPlainString()

        println("send $decimalToMaster to master")
        integrationHelper.sendEth(toMaster, integrationHelper.masterContract.contractAddress)
        println("Is locked: " + integrationHelper.masterContract.isLockAddPeer.send())

        var i = 0
        while (true) {
            println("===========================================")
            println("iteration ${i++}")

            val client = clients[0]

            println("Initial: ${client.getEthBalance()}")
            println("Client iroha balance ${client.getIrohaBalance()}")

            val amount = BigInteger.valueOf(12_000_000_000)
            client.deposit(amount)
            Thread.sleep(20_000)

            println("Before: ${client.getEthBalance()}")
            println("master eth balance ${integrationHelper.getEthBalance(integrationHelper.masterContract.contractAddress)}")
            println("Client iroha balance ${client.getIrohaBalance()}")

            val decimalAmount = BigDecimal(amount, ETH_PRECISION.toInt()).toPlainString()
            println("withdraw $decimalAmount")
            client.withdraw(decimalAmount)

            Thread.sleep(20_000)
            println("whitelist: " + integrationHelper.relayRegistryContract.getWhiteListByRelay(clients[0].relay).send())

            println("After: ${client.getEthBalance()}")

            println("Vacuum")
        }
    }
}
