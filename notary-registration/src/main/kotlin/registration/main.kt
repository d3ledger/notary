@file:JvmName("RegistrationMain")

package registration

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.getProfile
import mu.KLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import sidechain.iroha.IrohaInitialization

private val logger = KLogging().logger

@SpringBootApplication
@ComponentScan(basePackages = ["registration"])
class RegistrationApplication

/**
 * Entry point for notary registration service. Creates Iroha account for D3 clients.
 */
fun main(args: Array<String>) {
    IrohaInitialization.loadIrohaLibrary()
        .map {
            val app = SpringApplication(RegistrationApplication::class.java)
            app.setAdditionalProfiles(getProfile())
            app.setDefaultProperties(webPortProperties())
            app.run(*args)
        }.flatMap { context ->
            context.getBean(RegistrationServiceInitialization::class.java).init()
        }.failure { ex ->
            logger.error("Cannot run notary-registration", ex)
            System.exit(1)
        }
}

private fun webPortProperties(): Map<String, String> {
    val properties = HashMap<String, String>()
    properties["server.port"] = registrationConfig.healthCheckPort.toString()
    return properties
}
