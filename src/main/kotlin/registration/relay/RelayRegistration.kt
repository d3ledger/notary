package registration.relay

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.ConfigKeys
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelTransactionBuilder
import mu.KLogging
import notary.EthTokensProvider
import notary.EthTokensProviderImpl
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import java.math.BigInteger

/**
 * Class is responsible for relay addresses registration.
 * Deploys relay smart contracts in Ethereum network and records it in Iroha.
 */
class RelayRegistration(
    val ethTokensProvider: EthTokensProvider = EthTokensProviderImpl()
) {

    /** web3 service instance to communicate with Ethereum network */
    private val web3 = Web3j.build(HttpService(CONFIG[ConfigKeys.relayRegistartionEthConnectionUrl]))

    /** credentials of ethereum user */
    private val credentials = WalletUtils.loadCredentials(
        CONFIG[ConfigKeys.relayRegistartionEthCredentialPassword],
        CONFIG[ConfigKeys.relayRegistartionEthCredentialPath]
    )

    /** Gas price */
    private val gasPrice = BigInteger.valueOf(CONFIG[ConfigKeys.relayRegistartionEthGasPrice])

    /** Max gas limit */
    private val gasLimit = BigInteger.valueOf(CONFIG[ConfigKeys.relayRegistartionEthGasLimit])

    /** Iroha keypair */
    val keypair: Keypair = ModelUtil.loadKeypair(
        CONFIG[ConfigKeys.relayRegistrationPubkeyPath],
        CONFIG[ConfigKeys.relayRegistrationPrivkeyPath]
    ).get()

    /** Iroha host */
    val irohaHost = CONFIG[ConfigKeys.relayRegistrationIrohaHostname]

    /** Iroha port */
    val irohaPort = CONFIG[ConfigKeys.relayRegistrationIrohaPort]

    /** Iroha transaction creator */
    val creator = CONFIG[ConfigKeys.relayRegistrationIrohaAccount]

    /** Notary master account */
    val notaryIrohaAccount = CONFIG[ConfigKeys.relayRegistrationNotaryIrohaAccount]

    /**
     * Deploy user smart contract
     * @param master notary master account
     * @param tokens list of supported tokens
     * @return user smart contract address
     */
    private fun deployRelaySmartContract(master: String, tokens: List<String>): String {
        val contract =
            contract.Relay.deploy(
                web3,
                credentials,
                gasPrice,
                gasLimit,
                master,
                tokens
            ).send()

        logger.info { "Relay wallet created with address ${contract.contractAddress}" }
        return contract.contractAddress
    }

    /**
     * Sends transaction to Iroha.
     * @param wallet - ethereum wallet to record into Iroha
     * @return hex representation of transaction
     */
    private fun sendRelayToIroha(wallet: String): String {
        val currentTime = System.currentTimeMillis()
        val utx = ModelTransactionBuilder().creatorAccountId(creator)
            .createdTime(BigInteger.valueOf(currentTime))
            .setAccountDetail(notaryIrohaAccount, wallet, "free")
            .build()

        val hash = utx.hash()

        val protoTx = ModelUtil.prepareTransaction(utx, keypair)
        IrohaNetworkImpl(irohaHost, irohaPort).sendAndCheck(protoTx, hash)

        return hash.hex()
    }

    /**
     * Deploy [num] relay wallets
     * @param num - number of wallets to deploy
     */
    fun deploy(num: Int, master: String): Result<Unit, Exception> {
        return ethTokensProvider
            .getTokens()
            .map { token ->
                (1..num).forEach {
                    val relayWallet = deployRelaySmartContract(master, token.keys.toList())
                    sendRelayToIroha(relayWallet)
                }
            }
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
