import config.EthereumPasswords
import config.loadEthPasswords
import integration.helper.IntegrationHelperUtil
import jp.co.soramitsu.iroha.ModelCrypto
import model.IrohaCredential
import sidechain.eth.util.DeployHelper
import util.getRandomString

/**
 * Notary client.
 */
class NotaryClient(
    val integrationHelper: IntegrationHelperUtil,
    ethPasswordConfig: EthereumPasswords,
    val name: String = "client_${String.getRandomString(6)}"
) {
    val irohaCredential = IrohaCredential("$name@notary", ModelCrypto().generateKeypair())

    val eth = DeployHelper(
        integrationHelper.configHelper.createEthereumConfig("deploy/ethereum/keys/local/client0.key"),
        ethPasswordConfig
    )

    val whitelist: List<String> = listOf()

    // eth credentials

    private var balanceIroha: Map<String, Int> = mapOf("ether#ethereum" to 0)

    private val registrationAddress = "http://127.0.0.1:${integrationHelper.ethRegistrationConfig.port}"

    private val masterAccountId = integrationHelper.accountHelper.notaryAccount.accountId

    /**
     * Send HTTP POST request to registration service to register user
     * @param name - user name
     * @param pubkey - user public key
     * @param port - port of registration service
     */
    fun signUp(): khttp.responses.Response {
        return khttp.post(
            "$registrationAddress/users",
            data = mapOf(
                "name" to name,
                "whitelist" to whitelist.toString().trim('[').trim(']'),
                "pubkey" to irohaCredential.keyPair.publicKey().hex()
            )
        )
    }

    fun deposit() {}

    fun withdraw() {}

    fun transfer() {}
}
