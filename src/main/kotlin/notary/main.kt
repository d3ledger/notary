@file:JvmName("NotaryMain")

package notary

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.natpryce.konfig.ConfigurationProperties
import config.ConfigKeys
import mu.KLogging
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

/** Configuration parameters for notary instance */
val CONFIG = ConfigurationProperties.fromResource("defaults.properties")

/**
 * Application entry point
 */
fun main(args: Array<String>) {
    val logger = KLogging()

    IrohaInitialization.loadIrohaLibrary()
        .flatMap { ModelUtil.loadKeypair(CONFIG[ConfigKeys.notaryPubkeyPath], CONFIG[ConfigKeys.notaryPrivkeyPath]) }
        .flatMap { keypair ->
            val ethWalletsProvider = EthWalletsProviderIrohaImpl(
                CONFIG[ConfigKeys.notaryIrohaAccount],
                keypair,
                CONFIG[ConfigKeys.registrationServiceIrohaAccount],
                CONFIG[ConfigKeys.registrationServiceNotaryIrohaAccount]
            )
            NotaryInitialization(ethWalletsProvider).init()
        }
        .failure {
            logger.logger.error { it }
            System.exit(1)
        }
}
