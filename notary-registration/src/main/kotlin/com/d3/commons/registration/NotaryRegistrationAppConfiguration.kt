/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.registration

import com.d3.commons.config.loadLocalConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Utils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

val registrationConfig =
    loadLocalConfigs(
        "registration",
        NotaryRegistrationConfig::class.java,
        "registration.properties"
    ).get()

/**
 * Spring configuration for Notary Registration Service
 */
@Configuration
class NotaryRegistrationAppConfiguration {

    private val registrationKeyPair = Utils.parseHexKeypair(
        registrationConfig.registrationCredential.pubkey,
        registrationConfig.registrationCredential.privkey
    )

    /** Registartion service credentials */
    private val registrationCredential =
        IrohaCredential(registrationConfig.registrationCredential.accountId, registrationKeyPair)

    /** Iroha network connection */
    @Bean
    fun irohaAPI() = IrohaAPI(registrationConfig.iroha.hostname, registrationConfig.iroha.port)

    @Bean
    fun irohaConsumer() = IrohaConsumerImpl(
        registrationCredential, irohaAPI()
    )

    /** Configurations for Notary Registration Service */
    @Bean
    fun registrationConfig() = registrationConfig

    @Bean
    fun clientStorageAccount() = registrationConfig().clientStorageAccount

    @Bean
    fun brvsAccount() = registrationConfig().brvsAccount

    @Bean
    fun primaryKeyPair() =
        Utils.parseHexKeypair(
            registrationConfig.primaryPubkey,
            registrationConfig.primaryPrivkey
        )
}
