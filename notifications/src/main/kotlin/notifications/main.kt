@file:JvmName("NotificationsMain")

package notifications

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import mu.KLogging
import notifications.init.NotificationInitialization
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan

@ComponentScan(basePackages = ["notifications"])
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
