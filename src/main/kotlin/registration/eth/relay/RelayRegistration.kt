package registration.eth.relay

import com.github.kittinunf.result.Result
import config.EthereumPasswords
import mu.KLogging
import sidechain.eth.util.DeployHelper
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil

/**
 * Class is responsible for relay addresses registration.
 * Deploys relay smart contracts in Ethereum network and records it in Iroha.
 */
class RelayRegistration(
    private val relayRegistrationConfig: RelayRegistrationConfig,
    relayRegistrationEthereumPasswords: EthereumPasswords
) {
    /** Ethereum endpoint */
    private val deployHelper = DeployHelper(relayRegistrationConfig.ethereum, relayRegistrationEthereumPasswords)

    /** Iroha endpoint */
    private val irohaConsumer = IrohaConsumerImpl(relayRegistrationConfig.iroha)

    /** Iroha transaction creator */
    private val creator = relayRegistrationConfig.iroha.creator

    private val notaryIrohaAccount = relayRegistrationConfig.notaryIrohaAccount

    /**
     * Deploy user smart contract
     * @param master notary master account
     * @return user smart contract address
     */
    private fun deployRelaySmartContract(master: String): String {
        val contract = deployHelper.deployRelaySmartContract(master)
        logger.info { "Relay wallet created with address ${contract.contractAddress}" }
        return contract.contractAddress
    }

    /**
     * Registers relay in Iroha.
     * @param wallet - ethereum wallet to record into Iroha
     * @param creator - relay creator
     * @return Result with string representation of hash or possible failure
     */
    fun registerRelayIroha(wallet: String, creator: String): Result<String, Exception> {
        return ModelUtil.setAccountDetail(irohaConsumer, creator, notaryIrohaAccount, wallet, "free")
    }

    fun deploy(): Result<Unit, Exception> {
        return deploy(
            relayRegistrationConfig.number,
            relayRegistrationConfig.ethMasterWallet,
            creator
        )
    }

    fun deploy(
        relaysToDeploy: Int,
        ethMasterWallet: String,
        creator: String
    ): Result<Unit, Exception> {
        return Result.of {
            (1..relaysToDeploy).forEach {
                val relayWallet = deployRelaySmartContract(ethMasterWallet)
                registerRelayIroha(relayWallet, creator).fold(
                    { logger.info("Relay $relayWallet was deployed") },
                    { ex -> logger.error("Cannot deploy relay $relayWallet", ex) })
            }
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
