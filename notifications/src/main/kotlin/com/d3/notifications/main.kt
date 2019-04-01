@file:JvmName("NotificationsMain")

package com.d3.notifications

import com.d3.notifications.init.NotificationInitialization
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import mu.KLogging
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan

const val NOTIFICATIONS_SERVICE_NAME="notifications"

@ComponentScan(basePackages = ["com.d3.notifications"])
class NotificationApplication

private val logger = KLogging().logger

/*
  Notification service entry point
 */
fun main(args: Array<String>) {
    Result.of {
        AnnotationConfigApplicationContext(NotificationApplication::class.java)
    }.map { context ->
        context.getBean(NotificationInitialization::class.java).init { System.exit(1) }
    }.failure { ex ->
        logger.error("Cannot start notification service", ex)
        System.exit(1)
    }
}
