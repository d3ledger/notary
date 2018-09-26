package registration.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import config.EthereumPasswords
import contract.RelayRegistry
import okhttp3.OkHttpClient
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import provider.eth.EthFreeRelayProvider
import registration.IrohaAccountCreator
import registration.RegistrationStrategy
import sidechain.eth.util.BasicAuthenticator
import sidechain.iroha.consumer.IrohaConsumer
import java.math.BigInteger

/**
 * Effective implementation of [RegistrationStrategy]
 */
class EthRegistrationStrategyImpl(
    private val ethFreeRelayProvider: EthFreeRelayProvider,
    ethRegistrationConfig: EthRegistrationConfig,
    passwordConfig: EthereumPasswords,
    val irohaConsumer: IrohaConsumer,
    val notaryIrohaAccount: String,
    val creator: String
) : RegistrationStrategy {

    val credentials = WalletUtils.loadCredentials(
        passwordConfig.credentialsPassword,
        ethRegistrationConfig.ethereum.credentialsPath
    )!!
    val builder = OkHttpClient().newBuilder().authenticator(BasicAuthenticator(passwordConfig))!!
    val web3 = Web3j.build(HttpService(ethRegistrationConfig.ethereum.url, builder.build(), false))!!

    private val relayRegistry = RelayRegistry.load(
        ethRegistrationConfig.ethRelayRegistryAddress,
        web3,
        credentials,
        BigInteger.valueOf(ethRegistrationConfig.ethereum.gasPrice),
        BigInteger.valueOf(ethRegistrationConfig.ethereum.gasLimit)
    )



    /**
     * Register new notary client
     * @param name - client name
     * @param pubkey - client public key
     * @param whitelist - list of addresses from client
     * @return ethereum wallet has been registered
     */
    override fun register(name: String, whitelist: List<String>, pubkey: String): Result<String, Exception> {

        val irohaAccountCreator = IrohaAccountCreator(irohaConsumer, notaryIrohaAccount, creator, "ethereum_wallet")

        return ethFreeRelayProvider.getRelay()
            .flatMap { freeEthWallet ->
                relayRegistry.addNewRelayAddress(freeEthWallet, whitelist).send()
                irohaAccountCreator.create(
                    freeEthWallet, whitelist.toString().trim('[').trim(']'), name, pubkey
                )
            }
    }

}
