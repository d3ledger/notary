/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.config

import com.d3.commons.config.loadRawLocalConfigs
import com.d3.notifications.debug.DebugEndpoint
import com.dumbster.smtp.SimpleSmtpServer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

private val smtpConfig = loadRawLocalConfigs(
    "notifications.smtp",
    SMTPConfig::class.java, "smtp.properties"
)

@Profile("debug")
@Configuration
class DebugNotificationAppConfiguration {

    @Bean
    fun dumbster() = SimpleSmtpServer.start(smtpConfig.port)!!

    @Bean
    fun debugEndpoint(dumbster: SimpleSmtpServer, notificationsConfig: NotificationsConfig) =
        DebugEndpoint(dumbster, notificationsConfig)

}
