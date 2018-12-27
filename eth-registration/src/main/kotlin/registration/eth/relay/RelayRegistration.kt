package registration.eth.relay

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import config.EthereumPasswords
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import model.IrohaCredential
import mu.KLogging
import provider.eth.EthFreeRelayProvider
import sidechain.eth.util.DeployHelper
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.ModelUtil
import java.io.File
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.timer

/**
 * Class is responsible for relay addresses registration.
 * Deploys relay smart contracts in Ethereum network and records it in Iroha.
 */
class RelayRegistration(
    private val freeRelayProvider: EthFreeRelayProvider,
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

    fun deploy(
        relaysToDeploy: Int,
        ethMasterWallet: String
    ): Result<Unit, Exception> {
        logger.info { "Deploy $relaysToDeploy ethereum relays" }
        return Result.of {
            (1..relaysToDeploy).forEach { _ ->
                val relayWallet = deployRelaySmartContract(ethMasterWallet)
                registerRelayIroha(relayWallet).fold(
                    { logger.info("Relay $relayWallet was deployed") },
                    { ex -> logger.error("Cannot deploy relay $relayWallet", ex) })
            }
        }
    }

    /**
     * Run a job that every [period] checks that [relayRegistrationConfig.number] free relays are present. In case of
     * lack of free relays deploys lacking amount.
     */
    fun runRelayReplenishment(): Result<Unit, Exception> {
        logger.info { "Run relay replenishment" }

        return Result.of {
            while (true) {
                logger.info { "Relay replenishment triggered" }

                freeRelayProvider.getRelays().map { relays ->
                    logger.info { "Free relays: ${relays.size}" }
                    val toDeploy = relayRegistrationConfig.number - relays.size
                    if (toDeploy > 0)
                        deploy(toDeploy, relayRegistrationConfig.ethMasterWallet)
                }.failure { throw it }

                runBlocking { delay(relayRegistrationConfig.replenishmentPeriod * 1000) }
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
