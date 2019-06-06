/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.client

import com.google.gson.Gson
import nl.martijndwars.webpush.Subscription

const val D3_CLIENT_EMAIL_KEY = "email"
const val D3_CLIENT_ENABLE_NOTIFICATIONS = "notifications"
const val D3_CLIENT_PUSH_SUBSCRIPTION = "push_subscription"
private val gson = Gson()

/**
 * D3 client data class
 */
data class D3Client(
    val accountId: String,
    val email: String?,
    val enableNotifications: Boolean,
    val subscription: Subscription?
) {
    companion object {

        /**
         * Factory function that creates D3 client object using client account details
         * @param name - client accountId
         * @param details - account details taken from Iroha
         * @return d3 client
         */
        fun create(name: String, details: Map<String, String>): D3Client {
            val enableNotifications: Boolean
            details[D3_CLIENT_ENABLE_NOTIFICATIONS].let { notificationSetting ->
                enableNotifications = ("true" == notificationSetting)
            }
            val subscription: Subscription?
            details[D3_CLIENT_PUSH_SUBSCRIPTION].let { subscriptionJson ->
                if (subscriptionJson == null || subscriptionJson == "") {
                    subscription = null
                } else {
                    subscription = gson.fromJson(
                        details[D3_CLIENT_PUSH_SUBSCRIPTION],
                        Subscription::class.java
                    )
                }
            }
            return D3Client(
                name,
                details[D3_CLIENT_EMAIL_KEY],
                enableNotifications,
                subscription
            )
        }
    }
}
