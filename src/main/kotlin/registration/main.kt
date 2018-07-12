@file:JvmName("RegistrationMain")

package registration

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.natpryce.konfig.ConfigurationProperties
import mu.KLogging
import sidechain.iroha.IrohaInitialization

/** Configuration parameters for notary instance */
val CONFIG = ConfigurationProperties.fromResource("defaults.properties")

/**
 * Entry point for Registration Service
 */
fun main(args: Array<String>) {
    val logger = KLogging()

    IrohaInitialization.loadIrohaLibrary()
        .flatMap { RegistrationServiceInitialization().init() }
        .failure {
            logger.logger.error { it }
            System.exit(1)
        }
}
