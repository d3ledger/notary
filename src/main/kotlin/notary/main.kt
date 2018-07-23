@file:JvmName("NotaryMain")

package notary

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.EthereumPasswords
import config.loadConfigs
import mu.KLogging
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil

/**
 * Application entry point
 */
fun main(args: Array<String>) {
    val logger = KLogging()

    val notaryConfig = loadConfigs("notary", NotaryConfig::class.java)
    val passwordConfig =
        loadConfigs("notary", EthereumPasswords::class.java, "/ethereum_password.properties")

    val irohaNetwork = IrohaNetworkImpl(notaryConfig.iroha.hostname, notaryConfig.iroha.port)

    IrohaInitialization.loadIrohaLibrary()
        .flatMap { ModelUtil.loadKeypair(notaryConfig.iroha.pubkeyPath, notaryConfig.iroha.privkeyPath) }
        .flatMap { keypair ->
            val ethWalletsProvider = EthWalletsProviderIrohaImpl(
                notaryConfig.iroha,
                keypair,
                irohaNetwork,
                notaryConfig.iroha.creator,
                notaryConfig.registrationServiceIrohaAccount
            )
            NotaryInitialization(notaryConfig, passwordConfig, ethWalletsProvider).init()
        }
        .failure {
            logger.logger.error { it }
            System.exit(1)
        }
}
