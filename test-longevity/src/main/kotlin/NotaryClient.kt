import config.EthereumConfig
import config.EthereumPasswords
import integration.helper.IntegrationHelperUtil
import jp.co.soramitsu.iroha.ModelCrypto
import model.IrohaCredential
import mu.KLogging
import sidechain.eth.util.DeployHelper
import util.getRandomString
import java.math.BigInteger

/**
 * Notary client.
 */
class NotaryClient(
    val integrationHelper: IntegrationHelperUtil,
    ethConfig: EthereumConfig,
    ethPasswordConfig: EthereumPasswords,
    val name: String = "client_${String.getRandomString(6)}"
) {
    private val irohaCredential = IrohaCredential("$name@notary", ModelCrypto().generateKeypair())

    private val etherHelper = DeployHelper(ethConfig, ethPasswordConfig)

    private val masterAccountId = integrationHelper.accountHelper.notaryAccount.accountId

    val accountId = irohaCredential.accountId

    val ethAddress = etherHelper.credentials.address

    val whitelist: List<String> = listOf(ethAddress)

    var relay: String? = null

    init {
        logger.info { "Created client $accountId with eth address $ethAddress." }
    }

    /**
     * Send HTTP POST request to registration service to register user\
     */
    fun signUp(): khttp.responses.Response {
        val response = integrationHelper.sendRegistrationRequest(
            name,
            whitelist.toString(),
            irohaCredential.keyPair.publicKey(),
            integrationHelper.ethRegistrationConfig.port
        )
        relay = response.text

        return response
    }

    fun deposit(amount: BigInteger) {
        if (relay != null)
            etherHelper.sendEthereum(amount, relay!!)
        else
            throw Exception("Relay not registered.")
    }

    fun withdraw(amount: String) {
        logger.info { "Client $name wants to withdraw $amount ether" }
        integrationHelper.transferAssetIrohaFromClient(
            irohaCredential.accountId,
            irohaCredential.keyPair,
            irohaCredential.accountId,
            masterAccountId,
            "ether#ethereum",
            ethAddress,
            amount
        )
    }

    fun transfer(amount: String, to: String) {
        logger.info { "Client $name wants to transfer $amount ether to $to" }
        integrationHelper.transferAssetIrohaFromClient(
            irohaCredential.accountId,
            irohaCredential.keyPair,
            irohaCredential.accountId,
            to,
            "ether#ethereum",
            "",
            amount
        )
    }

    fun getEthBalance(): BigInteger {
        return integrationHelper.getEthBalance(ethAddress)
    }

    fun getIrohaBalance(): String {
        return integrationHelper.getIrohaAccountBalance(irohaCredential.accountId, "ether#ethereum")
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
