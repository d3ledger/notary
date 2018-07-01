package notary

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.natpryce.konfig.ConfigurationProperties
import mu.KLogging
import sidechain.iroha.IrohaInitialization

/** Configuration parameters for notary instance */
val CONFIG = ConfigurationProperties.fromResource("defaults.properties")

/**
 * Application entry point
 */
fun main(args: Array<String>) {
    val logger = KLogging()

    IrohaInitialization.loadIrohaLibrary()
        .flatMap { NotaryInitialization().init() }
        .failure {
            logger.logger.error { it }
            System.exit(1)
        }
}
