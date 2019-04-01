@file:JvmName("BtcRegistrationMain")

package com.d3.btc.registration

import com.d3.btc.registration.init.BtcRegistrationServiceInitialization
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.d3.commons.config.getProfile
import mu.KLogging
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan

private val logger = KLogging().logger
const val BTC_REGISTRATION_SERVICE_NAME="btc-registration"

@ComponentScan(basePackages = ["com.d3.btc.registration"])
class BtcRegistrationApplication

/**
 * Entry point for Registration Service
 */
fun main(args: Array<String>) {
    Result.of {
        val context = AnnotationConfigApplicationContext()
        context.environment.setActiveProfiles(getProfile())
        context.register(BtcRegistrationApplication::class.java)
        context.refresh()
        context
    }.flatMap { context ->
        context.getBean(BtcRegistrationServiceInitialization::class.java).init()
    }.failure { ex ->
        logger.error("Cannot run btc registration", ex)
        System.exit(1)
    }
}
