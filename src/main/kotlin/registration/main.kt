package registration

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import mu.KLogging

import sidechain.iroha.IrohaInitialization

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
