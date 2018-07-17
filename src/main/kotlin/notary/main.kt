@file:JvmName("NotaryMain")

package notary

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.loadConfigs
import mu.KLogging
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

/**
 * Application entry point
 */
fun main(args: Array<String>) {
    val logger = KLogging()

    val notaryConfig = loadConfigs("notary", NotaryConfig::class.java)

    IrohaInitialization.loadIrohaLibrary()
        .flatMap { ModelUtil.loadKeypair(notaryConfig.iroha.pubkeyPath, notaryConfig.iroha.privkeyPath) }
        .flatMap { keypair ->
            val ethWalletsProvider = EthWalletsProviderIrohaImpl(
                notaryConfig.iroha,
                keypair,
                notaryConfig.registrationServiceIrohaAccount,
                notaryConfig.iroha.creator
            )
            NotaryInitialization(notaryConfig, ethWalletsProvider).init()
        }
        .failure {
            logger.logger.error { it }
            System.exit(1)
        }
}
