/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.config

import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.provider.NotaryClientsProvider
import com.d3.commons.sidechain.iroha.IrohaChainListener
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
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

    private val notaryKeypair = Utils.parseHexKeypair(
        notificationsConfig.notaryCredential.pubkey,
        notificationsConfig.notaryCredential.privkey
    )

    private val notaryCredential =
        IrohaCredential(notificationsConfig.notaryCredential.accountId, notaryKeypair)

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
    fun notaryQueryHelper() = IrohaQueryHelperImpl(
        irohaAPI(),
        notificationsConfig.notaryCredential.accountId,
        notaryKeypair
    )

    @Bean
    fun irohaChainListener() = IrohaChainListener(irohaAPI(), notaryCredential)

    @Bean
    fun notificationsConfig() = notificationsConfig

    @Bean
    fun notaryClientsProvider() = NotaryClientsProvider(
        notaryQueryHelper(),
        notificationsConfig.clientStorageAccount,
        notificationsConfig.registrationServiceAccountName
    )

}
