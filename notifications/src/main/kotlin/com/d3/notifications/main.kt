/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("NotificationsMain")

package com.d3.notifications

import com.d3.commons.config.PROFILE_ENV
import com.d3.notifications.init.NotificationInitialization
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import mu.KLogging
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import kotlin.system.exitProcess

const val NOTIFICATIONS_SERVICE_NAME = "notifications"

@ComponentScan(basePackages = ["com.d3.notifications"])
class NotificationApplication

private val logger = KLogging().logger

/*
  Notification service entry point
 */
fun main() {
    Result.of {
        val context = AnnotationConfigApplicationContext()
        // Add profiles
        getProfiles().forEach {
            logger.info("Add profile $it")
            context.environment.addActiveProfile(it)
        }
        context.register(NotificationApplication::class.java)
        context.refresh()
        context
    }.flatMap { context ->
        context.getBean(NotificationInitialization::class.java).init { exitProcess(1) }
    }.failure { ex ->
        logger.error("Cannot start notification service", ex)
        exitProcess(1)
    }
}

/**
 * Returns current profiles based on environment variable
 */
fun getProfiles(): List<String> {
    // You may specify multiple profiles using ','
    var profileEnv = System.getenv(PROFILE_ENV)
    if (profileEnv == null) {
        logger.warn("No profile set. Using default profile")
        profileEnv = "d3"
    }
    return profileEnv.trim().split(", ").map { it.trim() }
}
