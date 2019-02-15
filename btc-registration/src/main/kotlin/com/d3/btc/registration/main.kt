@file:JvmName("BtcRegistrationMain")

package com.d3.btc.registration

import com.d3.btc.registration.config.btcRegistrationConfig
import com.d3.btc.registration.init.BtcRegistrationServiceInitialization
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.getProfile
import mu.KLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

private val logger = KLogging().logger

@SpringBootApplication
@ComponentScan(basePackages = ["com.d3.btc.registration"])
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
