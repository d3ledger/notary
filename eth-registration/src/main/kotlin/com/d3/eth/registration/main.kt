/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("EthRegistrationMain")

package com.d3.eth.registration

import com.d3.commons.config.ETH_RELAY_REGISTRY_ENV
import com.d3.commons.config.EthereumPasswords
import com.d3.commons.config.loadConfigs
import com.d3.commons.config.loadEthPasswords
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.IrohaAPI
import mu.KLogging

private val logger = KLogging().logger

const val ETH_REGISTRATION_SERVICE_NAME = "eth-registration"

/**
 * Entry point for Registration Service
 */
fun main(args: Array<String>) {
    loadConfigs("eth-registration", EthRegistrationConfig::class.java, "/eth/registration.properties")
        .map { ethRegistrationConfig ->
            object : EthRegistrationConfig {
                override val ethRelayRegistryAddress = System.getenv(ETH_RELAY_REGISTRY_ENV)
                    ?: ethRegistrationConfig.ethRelayRegistryAddress
                override val ethereum = ethRegistrationConfig.ethereum
                override val port = ethRegistrationConfig.port
                override val relayRegistrationIrohaAccount = ethRegistrationConfig.relayRegistrationIrohaAccount
                override val notaryIrohaAccount = ethRegistrationConfig.notaryIrohaAccount
                override val iroha = ethRegistrationConfig.iroha
                override val registrationCredential = ethRegistrationConfig.registrationCredential
            }
        }
        .fanout { loadEthPasswords("eth-registration", "/eth/ethereum_password.properties", args) }
        .map { (registrationConfig, passwordConfig) ->
            executeRegistration(registrationConfig, passwordConfig)
        }
}

fun executeRegistration(ethRegistrationConfig: EthRegistrationConfig, passwordConfig: EthereumPasswords) {
    logger.info { "Run ETH registration service" }
    val irohaNetwork = IrohaAPI(ethRegistrationConfig.iroha.hostname, ethRegistrationConfig.iroha.port)

    EthRegistrationServiceInitialization(ethRegistrationConfig, passwordConfig, irohaNetwork).init()
        .failure { ex ->
            logger.error("cannot run eth registration", ex)
            irohaNetwork.close()
            System.exit(1)
        }
}
