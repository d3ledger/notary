import config.IrohaCredentialConfig
import integration.helper.IntegrationHelperUtil
import sidechain.iroha.util.ModelUtil

class LongevityTest {
    private val integrationHelper = IntegrationHelperUtil()

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

    fun runServices() {
        runNotaries()
    }
}
