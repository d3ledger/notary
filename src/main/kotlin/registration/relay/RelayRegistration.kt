package registration.relay

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import config.EthereumPasswords
import mu.KLogging
import notary.EthTokensProvider
import notary.EthTokensProviderImpl
import sidechain.eth.util.DeployHelper
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil

/**
 * Class is responsible for relay addresses registration.
 * Deploys relay smart contracts in Ethereum network and records it in Iroha.
 */
class RelayRegistration(
    val relayRegistrationConfig: RelayRegistrationConfig,
    relayRegistrationEthereumPasswords: EthereumPasswords
) {

    /** Ethereum token list provider */
    val ethTokensProvider: EthTokensProvider = EthTokensProviderImpl(relayRegistrationConfig.db)

    /** Ethereum endpoint */
    val deployHelper = DeployHelper(relayRegistrationConfig.ethereum, relayRegistrationEthereumPasswords)

    /** Iroha endpoint */
    val irohaConsumer = IrohaConsumerImpl(relayRegistrationConfig.iroha)

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
    private fun deployRelaySmartContract(master: String): String {
        val contract = deployHelper.deployRelaySmartContract(master)

        logger.info { "Relay wallet created with address ${contract.contractAddress}" }
        return contract.contractAddress
    }

    /**
     * Sends transaction to Iroha.
     * @param wallet - ethereum wallet to record into Iroha
     * @return Result with string representation of hash or possible failure
     */
    private fun sendRelayToIroha(wallet: String): Result<String, Exception> {
        return ModelUtil.setAccountDetail(irohaConsumer, creator, notaryIrohaAccount, wallet, "free")
    }

    /**
     * Deploy relay wallets
     */
    fun deploy(): Result<Unit, Exception> {
        return ethTokensProvider
            .getTokens()
            .map { tokens ->
                (1..relayRegistrationConfig.number).forEach {
                    val relayWallet =
                        deployRelaySmartContract(relayRegistrationConfig.ethMasterWallet)
                    sendRelayToIroha(relayWallet)
                        .failure { logger.error { it } }
                }
            }
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
