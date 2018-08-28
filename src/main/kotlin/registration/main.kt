@file:JvmName("RegistrationMain")

package registration

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.loadConfigs
import mu.KLogging
import sidechain.iroha.IrohaInitialization

/**
 * Entry point for Registration Service
 */
fun main(args: Array<String>) {
    val registrationConfig = loadConfigs("registration", RegistrationConfig::class.java, "/registration.properties")
    executeRegistration(registrationConfig)
}

fun executeRegistration(registrationConfig: RegistrationConfig) {
    val logger = KLogging()

    IrohaInitialization.loadIrohaLibrary()
        .flatMap { RegistrationServiceInitialization(registrationConfig).init() }
        .failure {
            logger.logger.error { it }
            System.exit(1)
        }
}
