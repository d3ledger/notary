package registration.eth.relay

import com.github.kittinunf.result.Result
import config.EthereumPasswords
import model.IrohaCredential
import mu.KLogging
import sidechain.eth.util.DeployHelper
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.ModelUtil
import java.io.File

/**
 * Class is responsible for relay addresses registration.
 * Deploys relay smart contracts in Ethereum network and records it in Iroha.
 */
class RelayRegistration(
    private val relayRegistrationConfig: RelayRegistrationConfig,
    relayCredential: IrohaCredential,
    irohaNetwork: IrohaNetwork,
    relayRegistrationEthereumPasswords: EthereumPasswords
) {
    /** Ethereum endpoint */
    private val deployHelper = DeployHelper(relayRegistrationConfig.ethereum, relayRegistrationEthereumPasswords)

    /** Iroha endpoint */
    private val irohaConsumer = IrohaConsumerImpl(relayCredential, irohaNetwork)

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
     * @param relayAddress - relay address to record into Iroha
     * @return Result with string representation of hash or possible failure
     */
    fun registerRelayIroha(relayAddress: String): Result<String, Exception> {
        return ModelUtil.setAccountDetail(irohaConsumer, notaryIrohaAccount, relayAddress, "free")
    }

    fun deploy(): Result<Unit, Exception> {
        return deploy(
            relayRegistrationConfig.number,
            relayRegistrationConfig.ethMasterWallet
        )
    }

    fun deploy(
        relaysToDeploy: Int,
        ethMasterWallet: String
    ): Result<Unit, Exception> {
        return Result.of {
            (1..relaysToDeploy).forEach { _ ->
                val relayWallet = deployRelaySmartContract(ethMasterWallet)
                registerRelayIroha(relayWallet).fold(
                    { logger.info("Relay $relayWallet was deployed") },
                    { ex -> logger.error("Cannot deploy relay $relayWallet", ex) })
            }
        }
    }

    fun import(filename: String): Result<Unit, Exception> {
        return Result.of {
            getRelaysFromFile(filename).forEach { relay ->
                registerRelayIroha(relay).fold(
                    { logger.info("Relay $relay was imported") },
                    { ex -> logger.error("Cannot import relay $relay", ex) })
            }
        }
    }

    private fun getRelaysFromFile(filename: String): List<String> {
        return File(filename).readLines()
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
