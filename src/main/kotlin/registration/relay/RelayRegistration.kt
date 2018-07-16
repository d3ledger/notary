package registration.relay

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
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
    val relayRegistrationConfig: RelayRegistrationConfig,
    val ethTokensProvider: EthTokensProvider = EthTokensProviderImpl()
) {

    /** web3 service instance to communicate with Ethereum network */
    private val web3 = Web3j.build(HttpService(relayRegistrationConfig.ethereum.url))

    /** credentials of ethereum user */
    private val credentials = WalletUtils.loadCredentials(
        relayRegistrationConfig.ethereum.credentialsPassword,
        relayRegistrationConfig.ethereum.credentialsPath
    )

    /** Gas price */
    private val gasPrice = BigInteger.valueOf(relayRegistrationConfig.ethereum.gasPrice)

    /** Max gas limit */
    private val gasLimit = BigInteger.valueOf(relayRegistrationConfig.ethereum.gasLimit)

    /** Iroha keypair */
    val keypair: Keypair = ModelUtil.loadKeypair(
        relayRegistrationConfig.iroha.pubkeyPath,
        relayRegistrationConfig.iroha.privkeyPath
    ).get()

    /** Iroha host */
    val irohaHost = relayRegistrationConfig.iroha.hostname

    /** Iroha port */
    val irohaPort = relayRegistrationConfig.iroha.port

    /** Iroha transaction creator */
    val creator = relayRegistrationConfig.iroha.creator

    /** Notary master account */
    val notaryIrohaAccount = relayRegistrationConfig.notaryIrohaAccount

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
     * Deploy relay wallets
     */
    fun deploy(): Result<Unit, Exception> {
        return ethTokensProvider
            .getTokens()
            .map { token ->
                (1..relayRegistrationConfig.number).forEach {
                    val relayWallet =
                        deployRelaySmartContract(relayRegistrationConfig.ethMasterWallet, token.keys.toList())
                    sendRelayToIroha(relayWallet)
                }
            }
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
