package registration.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.EthereumPasswords
import contract.RelayRegistry
import mu.KLogging
import notary.endpoint.eth.ETH_WHITE_LIST_KEY
import okhttp3.OkHttpClient
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.StaticGasProvider
import provider.eth.EthFreeRelayProvider
import provider.eth.EthRelayProvider
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
    private val ethRelayProvider: EthRelayProvider,
    ethRegistrationConfig: EthRegistrationConfig,
    passwordConfig: EthereumPasswords,
    private val irohaConsumer: IrohaConsumer,
    private val notaryIrohaAccount: String
) : RegistrationStrategy {

    init {
        logger.info { "Init EthRegistrationStrategyImpl with irohaCreator=${irohaConsumer.creator}, notaryIrohaAccount=$notaryIrohaAccount" }
    }

    private val irohaAccountCreator =
        IrohaAccountCreator(irohaConsumer, notaryIrohaAccount, "ethereum_wallet")

    private val credentials = WalletUtils.loadCredentials(
        passwordConfig.credentialsPassword,
        ethRegistrationConfig.ethereum.credentialsPath
    )!!
    private val builder = OkHttpClient().newBuilder().authenticator(BasicAuthenticator(passwordConfig))!!
    private val web3 = Web3j.build(HttpService(ethRegistrationConfig.ethereum.url, builder.build(), false))!!

    private val relayRegistry = RelayRegistry.load(
        ethRegistrationConfig.ethRelayRegistryAddress,
        web3,
        credentials,
        StaticGasProvider(
            BigInteger.valueOf(ethRegistrationConfig.ethereum.gasPrice),
            BigInteger.valueOf(ethRegistrationConfig.ethereum.gasLimit)
        )
    )

    /**
     * Register new notary client
     * @param accountName - client name
     * @param domainId - client domain
     * @param pubkey - client public key
     * @param whitelist - list of addresses from client
     * @return ethereum wallet has been registered
     */
    override fun register(
        accountName: String,
        domainId: String,
        whitelist: List<String>,
        pubkey: String
    ): Result<String, Exception> {
        return ethFreeRelayProvider.getRelay()
            .flatMap { freeEthWallet ->
                ethRelayProvider.getRelaysByAccountId("$accountName@$domainId")
                    .flatMap { assignedRelays ->
                        // check that client hasn't been registered yet
                        if (!assignedRelays.isEmpty())
                            throw IllegalArgumentException("Client $accountName@$domainId has already been registered with relay: $assignedRelays")

                        // register to Ethereum RelayRegistry
                        logger.info { "Add new relay to relay registry relayRegistry=$relayRegistry, freeWallet=$freeEthWallet, whitelist=$whitelist, creator=${credentials.address}." }
                        relayRegistry.addNewRelayAddress(freeEthWallet, whitelist).send()

                        // register to Iroha
                        irohaAccountCreator.create(
                            freeEthWallet,
                            ETH_WHITE_LIST_KEY,
                            whitelist,
                            accountName,
                            domainId,
                            pubkey
                        ) { "$accountName@$domainId" }
                    }
            }
    }

    /**
     * Return number of free relays.
     */
    override fun getFreeAddressNumber(): Result<Int, Exception> {
        return ethFreeRelayProvider.getRelays()
            .map { freeRelays ->
                freeRelays.size
            }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
