/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.config

import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.notifications.provider.D3ClientProvider
import com.d3.notifications.push.PushServiceFactory
import com.d3.notifications.push.WebPushAPIService
import com.d3.notifications.push.WebPushAPIServiceImpl
import com.d3.notifications.rest.DumbsterEndpoint
import com.d3.notifications.service.EmailNotificationService
import com.d3.notifications.service.PushNotificationService
import com.d3.notifications.smtp.SMTPService
import com.d3.notifications.smtp.SMTPServiceImpl
import com.dumbster.smtp.SimpleSmtpServer
import nl.martijndwars.webpush.PushService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

private val smtpConfig = loadRawLocalConfigs(
    "notifications.smtp",
    SMTPConfig::class.java, "smtp.properties"
)

private val pushConfig = loadRawLocalConfigs(
    "notifications.push",
    PushAPIConfig::class.java, "push.properties"
)

@Profile("d3")
@Configuration
class D3NotificationsAppConfiguration {
    @Bean
    fun smtpService() = SMTPServiceImpl(smtpConfig)

    @Bean
    fun d3ClientProvider(
        @Qualifier("notaryQueryHelper")
        notaryQueryHelper: IrohaQueryHelper
    ) = D3ClientProvider(notaryQueryHelper)

    @Bean
    fun pushServiceFactory() = object : PushServiceFactory {
        override fun create() =
            PushService(pushConfig.vapidPubKeyBase64, pushConfig.vapidPrivKeyBase64, "D3 notifications")
    }

    @Bean
    fun dumbster() = SimpleSmtpServer.start(smtpConfig.port)!!

    @Bean
    fun dumbsterEndpoint(dumbster: SimpleSmtpServer, notificationsConfig: NotificationsConfig) =
        DumbsterEndpoint(dumbster, notificationsConfig)

    @Bean
    fun emailNotificationService(smtpService: SMTPService, d3ClientProvider: D3ClientProvider) =
        EmailNotificationService(smtpService, d3ClientProvider)

    @Bean
    fun webPushAPIService(d3ClientProvider: D3ClientProvider, pushServiceFactory: PushServiceFactory) =
        WebPushAPIServiceImpl(d3ClientProvider, pushServiceFactory)

    @Bean
    fun pushNotificationService(webPushAPIService: WebPushAPIService) = PushNotificationService(webPushAPIService)
}
