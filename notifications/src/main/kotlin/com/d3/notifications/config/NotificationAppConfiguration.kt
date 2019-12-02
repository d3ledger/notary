/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.config

import com.d3.chainadapter.client.RMQConfig
import com.d3.chainadapter.client.ReliableIrohaChainListener
import com.d3.chainadapter.client.createPrettySingleThreadPool
import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.healthcheck.HealthCheckEndpoint
import com.d3.commons.provider.NotaryClientsProvider
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.sidechain.iroha.util.impl.RobustIrohaQueryHelperImpl
import com.d3.notifications.NOTIFICATIONS_SERVICE_NAME
import com.d3.notifications.service.SoraNotificationService
import io.grpc.ManagedChannelBuilder
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Utils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val notificationsConfig = loadRawLocalConfigs(
    "notifications",
    NotificationsConfig::class.java, "notifications.properties"
)

@Configuration
class NotificationAppConfiguration {

    private val notaryKeypair = Utils.parseHexKeypair(
        notificationsConfig.notaryCredential.pubkey,
        notificationsConfig.notaryCredential.privkey
    )

    @Bean
    fun chainListenerExecutorService() = createPrettySingleThreadPool(
        NOTIFICATIONS_SERVICE_NAME, "iroha-chain-listener"
    )

    @Bean
    fun irohaAPI(): IrohaAPI {
        val irohaAPI = IrohaAPI(notificationsConfig.iroha.hostname, notificationsConfig.iroha.port)
        irohaAPI.setChannelForStreamingQueryStub(
            ManagedChannelBuilder.forAddress(
                notificationsConfig.iroha.hostname, notificationsConfig.iroha.port
            ).directExecutor().usePlaintext().build()
        )
        return irohaAPI
    }

    @Bean
    fun notaryQueryHelper() = RobustIrohaQueryHelperImpl(
        IrohaQueryHelperImpl(
            irohaAPI(),
            notificationsConfig.notaryCredential.accountId,
            notaryKeypair
        ), notificationsConfig.irohaQueryTimeoutMls
    )

    @Bean
    fun irohaChainListener() = ReliableIrohaChainListener(
        rmqConfig = notificationsConfig.rmq,
        irohaQueue = notificationsConfig.blocksQueue,
        autoAck = false,
        consumerExecutorService = chainListenerExecutorService()
    )

    @Bean
    fun rmqConfig() = notificationsConfig.rmq

    @Bean
    fun notificationsConfig() = notificationsConfig

    @Bean
    fun notaryClientsProvider() = NotaryClientsProvider(
        notaryQueryHelper(),
        notificationsConfig.clientStorageAccount,
        notificationsConfig.registrationServiceAccountName
    )

    @Bean
    fun healthCheckEndpoint() = HealthCheckEndpoint(notificationsConfig.healthCheckPort)

    @Bean
    fun soraNotificationService(rmqConfig: RMQConfig) = SoraNotificationService(rmqConfig)
}
