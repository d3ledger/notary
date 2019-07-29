/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.push

import com.d3.notifications.provider.D3ClientProvider
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import mu.KLogging
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import nl.martijndwars.webpush.Subscription
import org.apache.http.util.EntityUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.security.Security

/**
 * Service that used to send web push notifications using Push API
 */
@Component
class WebPushAPIServiceImpl(
    @Autowired private val d3ClientProvider: D3ClientProvider,
    @Autowired private val pushServiceFactory: PushServiceFactory
) : WebPushAPIService {

    override fun push(accountId: String, message: String): Result<Unit, Exception> {
        return d3ClientProvider.getClient(accountId)
            .map { d3Client ->
                if (!d3Client.enableNotifications) {
                    logger.warn {
                        "Cannot send push notification. Client $accountId doesn't want to be notified"
                    }
                } else if (d3Client.subscription == null) {
                    logger.warn {
                        "Cannot send push notification. Client $accountId has no push subscription data"
                    }
                } else {
                    sendPushMessage(d3Client.subscription, message)
                    logger.info { "Push message '$message' has been successfully sent to $accountId" }
                }
            }
    }

    // Sends push message to Push Service
    private fun sendPushMessage(sub: Subscription, message: String) {
        Security.addProvider(BouncyCastleProvider())
        // Create a notification with the endpoint, userPublicKey from the subscription and a custom payload
        val notification = Notification(sub, message)
        // Instantiate the push service, no need to use an API key for Push API
        val pushService = pushServiceFactory.create()
        // Send the notification
        val response = pushService.send(notification)
        val statusCode = response.statusLine.statusCode
        //HTTP 'success' statuses are in range [200;226]
        // See https://en.wikipedia.org/wiki/List_of_HTTP_status_codes
        if (statusCode !in 200..226) {
            val responseMessage = EntityUtils.toString(response.entity, "UTF-8")
            throw IllegalAccessException(
                "Cannot send push due to error: $responseMessage"
            )
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}

interface PushServiceFactory {
    fun create(): PushService
}
