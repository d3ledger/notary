@file:JvmName("RegistrationMain")

package com.d3.commons.registration

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.d3.commons.config.getProfile
import mu.KLogging
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan

private val logger = KLogging().logger

@ComponentScan(basePackages = ["com.d3.commons.registration"])
class RegistrationApplication

/**
 * Entry point for notary registration service. Creates Iroha account for D3 clients.
 */
fun main(args: Array<String>) {
    Result.of {
        val context = AnnotationConfigApplicationContext()
        context.environment.setActiveProfiles(getProfile())
        context.register(RegistrationApplication::class.java)
        context.refresh()
        context
    }.flatMap { context ->
        context.getBean(RegistrationServiceInitialization::class.java).init()
    }.failure { ex ->
        logger.error("Cannot run notary-registration", ex)
        System.exit(1)
    }
}

