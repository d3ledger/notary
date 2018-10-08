import com.github.kittinunf.result.Result
import config.IrohaCredentialConfig
import config.loadEthPasswords
import integration.helper.IntegrationHelperUtil
import sidechain.iroha.util.ModelUtil

class LongevityTest {
    private val integrationHelper = IntegrationHelperUtil()

    /** 5 notary clients */
    val clients = listOf(
        NotaryClient(integrationHelper, loadEthPasswords("client0", "/eth/ethereum_password.properties")),
        NotaryClient(integrationHelper, loadEthPasswords("client1", "/eth/ethereum_password.properties")),
        NotaryClient(integrationHelper, loadEthPasswords("client2", "/eth/ethereum_password.properties")),
        NotaryClient(integrationHelper, loadEthPasswords("client3", "/eth/ethereum_password.properties")),
        NotaryClient(integrationHelper, loadEthPasswords("client4", "/eth/ethereum_password.properties"))
    )

    /** Run 4 instances of notary */
    private fun runNotaries() {
        integrationHelper.runEthNotary()

        val irohaConfig = integrationHelper.configHelper.createIrohaConfig()
        val etherConfig = integrationHelper.configHelper.createEthereumConfig()

        (1..3).forEach {
            val notaryCredential = object : IrohaCredentialConfig {
                override val pubkeyPath = "deploy/iroha/keys/notary$it@notary.pub"
                override val privkeyPath = "deploy/iroha/keys/notary$it@notary.priv"
                override val accountId = integrationHelper.accountHelper.notaryAccount.accountId
            }
            val notaryConfig =
                integrationHelper.configHelper.createEthNotaryConfig(irohaConfig, etherConfig, notaryCredential)

            integrationHelper.accountHelper.addNotarySignatory(
                ModelUtil.loadKeypair(
                    notaryCredential.pubkeyPath,
                    notaryCredential.privkeyPath
                ).get()
            )

            integrationHelper.runEthNotary(notaryConfig)
        }
    }

    /**
     * Run all notary services.
     */
    fun runServices() {
        runNotaries()
        integrationHelper.runRegistrationService()
        integrationHelper.runEthWithdrawalService()

        // wait until services are up
        Thread.sleep(10_000)
    }

    fun registerClients(): Result<Unit, Exception> {
        return Result.of {
            for (client in clients) {
                val status = client.signUp().statusCode
                if (status != 200)
                    throw Exception("Cannot register client ${client.irohaCredential.accountId}, status code: $status")
            }
        }
    }

    fun run() {
        runServices()
        registerClients()
    }
}
