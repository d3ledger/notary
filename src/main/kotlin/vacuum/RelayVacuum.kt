package vacuum

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.EthereumPasswords
import contract.Relay
import jp.co.soramitsu.iroha.Keypair
import mu.KLogging
import notary.EthWalletsProviderIrohaImpl
import sidechain.eth.util.DeployHelper
import sidechain.iroha.consumer.IrohaNetworkImpl

/**
 * Class is responsible for relay addresses vacuum.
 *  Sends all tokens from relay smart contracts to master contract in Ethereum network and records it in Iroha.
 */
class RelayVacuum(
    relayVacuumConfig: RelayVacuumConfig,
    relayVacuumEthereumPasswords: EthereumPasswords,
    keypair: Keypair
) {
    private val ethTokenAddress = "0"
    private val irohaNetwork = IrohaNetworkImpl(relayVacuumConfig.iroha.hostname, relayVacuumConfig.iroha.port)

    /** Ethereum endpoint */
    private val deployHelper = DeployHelper(relayVacuumConfig.ethereum, relayVacuumEthereumPasswords)

    private val ethWalletsProvider = EthWalletsProviderIrohaImpl(
        relayVacuumConfig.iroha,
        keypair,
        irohaNetwork,
        relayVacuumConfig.registrationServiceIrohaAccount,
        relayVacuumConfig.registrationServiceIrohaAccount
    )

    private fun getAllRelays(): Result<List<Relay>, Exception> {
        return ethWalletsProvider.getWallets().map { wallets ->
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

    fun vacuum(): Result<Unit, Exception> {
        return getAllRelays().map { relays ->
            relays.forEach { relay ->
                relay.sendToMaster(ethTokenAddress)
            }
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()

}