@file:JvmName("BtcRegistrationMain")

package registration.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.getProfile
import mu.KLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import registration.btc.config.btcRegistrationConfig
import registration.btc.init.BtcRegistrationServiceInitialization

private val logger = KLogging().logger

@SpringBootApplication
@ComponentScan(basePackages = ["registration"])
class BtcRegistrationApplication

/**
 * Entry point for Registration Service
 */
fun main(args: Array<String>) {
    Result.of {
        val app = SpringApplication(BtcRegistrationApplication::class.java)
        app.setAdditionalProfiles(getProfile())
        app.setDefaultProperties(webPortProperties())
        app.run(*args)
    }.flatMap { context ->
        context.getBean(BtcRegistrationServiceInitialization::class.java).init()
    }.failure { ex ->
        logger.error("Cannot run btc registration", ex)
        System.exit(1)
    }
}

private fun webPortProperties(): Map<String, String> {
    val properties = HashMap<String, String>()
    properties["server.port"] = btcRegistrationConfig.healthCheckPort.toString()
    return properties
}
