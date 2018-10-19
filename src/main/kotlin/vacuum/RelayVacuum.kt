package vacuum

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.EthereumPasswords
import contract.Relay
import model.IrohaCredential
import mu.KLogging
import provider.eth.EthRelayProviderIrohaImpl
import provider.eth.EthTokensProviderImpl
import sidechain.eth.util.DeployHelper
import sidechain.iroha.consumer.IrohaNetwork

/**
 * Class is responsible for relay contracts vacuum
 * Sends all tokens from relay smart contracts to master contract in Ethereum network
 */
class RelayVacuum(
    relayVacuumConfig: RelayVacuumConfig,
    relayVacuumEthereumPasswords: EthereumPasswords,
    credential: IrohaCredential,
    irohaNetwork: IrohaNetwork
) {
    private val ethTokenAddress = "0x0000000000000000000000000000000000000000"

    /** Ethereum endpoint */
    private val deployHelper = DeployHelper(relayVacuumConfig.ethereum, relayVacuumEthereumPasswords)
    private val ethTokensProvider = EthTokensProviderImpl(
        credential,
        irohaNetwork,
        relayVacuumConfig.tokenStorageAccount,
        relayVacuumConfig.tokenSetterAccount
    )

    private val ethRelayProvider = EthRelayProviderIrohaImpl(
        irohaNetwork,
        credential,
        relayVacuumConfig.notaryIrohaAccount,
        relayVacuumConfig.registrationServiceIrohaAccount
    )

    /**
     * Returns all non free relays
     */
    private fun getAllRelays(): Result<List<Relay>, Exception> {
        return ethRelayProvider.getRelays().map { wallets ->
            wallets.keys.map { ethPublicKey ->
                Relay.load(
                    ethPublicKey,
                    deployHelper.web3,
                    deployHelper.credentials,
                    deployHelper.gasPrice,
                    deployHelper.gasLimit
                )
            }
        }
    }

    /**
     * Moves all currency(ETH and tokens) from non free relay contracts to master contract
     */
    fun vacuum(): Result<Unit, Exception> {
        return ethTokensProvider.getTokens().flatMap { providedTokens ->
            logger.info { "Provided tokens $providedTokens" }
            getAllRelays().map { relays ->
                logger.info { "Relays to vacuum ${relays.map { relay -> relay.contractAddress }}" }
                relays.forEach { relay ->
                    relay.sendToMaster(ethTokenAddress).send()
                    logger.info("${relay.contractAddress} send to master eth $ethTokenAddress")
                    providedTokens.forEach { providedToken ->
                        logger.info("${relay.contractAddress} send to master ${providedToken.value} ${providedToken.key}")
                        relay.sendToMaster(providedToken.key).send()
                    }
                }
            }
        }
    }

    /**
     * Logger
     */
    private companion object : KLogging()
}
