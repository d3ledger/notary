/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.config

import com.d3.commons.config.loadRawLocalConfigs
import com.d3.notifications.service.SoraNotificationService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

private val soraConfig = loadRawLocalConfigs(
    "notifications.sora",
    SoraConfig::class.java, "sora.properties"
)

@Profile("sora")
@Configuration
class SoraNotificationsAppConfiguration {

    @Bean
    fun soraNotificationService() = SoraNotificationService(soraConfig)
}
