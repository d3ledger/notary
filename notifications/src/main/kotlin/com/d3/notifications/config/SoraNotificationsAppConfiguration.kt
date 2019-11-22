/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.config

import com.d3.chainadapter.client.RMQConfig
import com.d3.notifications.service.SoraNotificationService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile


@Profile("sora")
@Configuration
class SoraNotificationsAppConfiguration {

    @Bean
    fun soraNotificationService(rmqConfig: RMQConfig) = SoraNotificationService(rmqConfig)
}
