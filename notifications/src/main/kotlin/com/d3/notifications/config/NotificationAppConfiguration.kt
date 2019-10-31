/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.config

import com.d3.chainadapter.client.ReliableIrohaChainListener
import com.d3.chainadapter.client.createPrettySingleThreadPool
import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.provider.NotaryClientsProvider
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.sidechain.iroha.util.impl.RobustIrohaQueryHelperImpl
import com.d3.commons.util.createPrettyFixThreadPool
import com.d3.notifications.NOTIFICATIONS_SERVICE_NAME
import io.grpc.ManagedChannelBuilder
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Utils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

val notificationsConfig = loadRawLocalConfigs(
    "notifications",
    NotificationsConfig::class.java, "notifications.properties"
)

@Configuration
class NotificationAppConfiguration {

    private val notificationsKeypair = Utils.parseHexKeypair(
        notificationsConfig.notificationCredential.pubkey,
        notificationsConfig.notificationCredential.privkey
    )

    private val notificationsIrohaCredential =
        IrohaCredential(notificationsConfig.notificationCredential.accountId, notificationsKeypair)

    @Bean
    fun chainListenerExecutorService() = createPrettyFixThreadPool(NOTIFICATIONS_SERVICE_NAME, "iroha-chain-listener")

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
    fun notificationsIrohaConsumer() = IrohaConsumerImpl(notificationsIrohaCredential, irohaAPI())

    @Bean
    fun notificationsQueryHelper() = RobustIrohaQueryHelperImpl(
        IrohaQueryHelperImpl(
            irohaAPI(),
            notificationsConfig.notificationCredential.accountId,
            notificationsKeypair
        ), notificationsConfig.irohaQueryTimeoutMls
    )

    //TODO set basic QOS
    @Bean
    fun irohaChainListener() =
        ReliableIrohaChainListener(
            rmqConfig = notificationsConfig.rmq,
            irohaQueue = notificationsConfig.blocksQueue,
            autoAck = true,
            consumerExecutorService = chainListenerExecutorService()
        )

    @Bean
    fun notificationsConfig() = notificationsConfig

    @Bean
    fun notaryClientsProvider() = NotaryClientsProvider(
        notificationsQueryHelper(),
        notificationsConfig.clientStorageAccount,
        notificationsConfig.registrationServiceAccountName
    )

}
