package vacuum

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.EthereumPasswords
import contract.Relay
import jp.co.soramitsu.iroha.Keypair
import mu.KLogging
import provider.EthRelayProviderIrohaImpl
import provider.EthTokensProviderImpl
import sidechain.eth.util.DeployHelper

/**
 * Class is responsible for relay contracts vacuum
 * Sends all tokens from relay smart contracts to master contract in Ethereum network
 */
class RelayVacuum(
    relayVacuumConfig: RelayVacuumConfig,
    relayVacuumEthereumPasswords: EthereumPasswords,
    keypair: Keypair
) {
    private val ethTokenAddress = "0x0000000000000000000000000000000000000000"

    /** Ethereum endpoint */
    private val deployHelper = DeployHelper(relayVacuumConfig.ethereum, relayVacuumEthereumPasswords)

    private val ethTokensProvider = EthTokensProviderImpl(
        relayVacuumConfig.iroha,
        keypair,
        relayVacuumConfig.notaryIrohaAccount,
        relayVacuumConfig.tokenStorageAccount
    )

    private val ethRelayProvider = EthRelayProviderIrohaImpl(
        relayVacuumConfig.iroha,
        keypair,
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
            getAllRelays().map { relays ->
                relays.forEach { relay ->
                    relay.sendToMaster(ethTokenAddress).send()
                    logger.info("${relay.contractAddress} sendToMaster $ethTokenAddress")
                    providedTokens.keys.forEach { providedToken ->
                        logger.info("${relay.contractAddress} sendToMaster $providedToken")
                        relay.sendToMaster(providedToken).send()
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
