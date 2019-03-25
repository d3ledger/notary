/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.registration

import com.d3.commons.config.EthereumPasswords
import com.d3.commons.registration.IrohaEthAccountRegistrator
import com.d3.commons.registration.RegistrationStrategy
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.eth.provider.EthFreeRelayProvider
import com.d3.eth.provider.EthRelayProvider
import com.d3.eth.sidechain.util.DeployHelper
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import mu.KLogging

/**
 * Effective implementation of [RegistrationStrategy]
 */
class EthRegistrationStrategyImpl(
    private val ethFreeRelayProvider: EthFreeRelayProvider,
    private val ethRelayProvider: EthRelayProvider,
    private val ethRegistrationConfig: EthRegistrationConfig,
    ethPasswordConfig: EthereumPasswords,
    private val irohaConsumer: IrohaConsumer,
    private val notaryIrohaAccount: String
) : RegistrationStrategy {

    init {
        logger.info { "Init EthRegistrationStrategyImpl with irohaCreator=${irohaConsumer.creator}, notaryIrohaAccount=$notaryIrohaAccount" }
    }

    private val ethereumAccountRegistrator =
        IrohaEthAccountRegistrator(irohaConsumer, notaryIrohaAccount)

    private val deployHelper = DeployHelper(ethRegistrationConfig.ethereum, ethPasswordConfig)

    /**
     * Register new notary client
     * @param accountName - client name
     * @param domainId - client domain
     * @param publicKey - client public key
     * @param whitelist - list of addresses from client
     * @return ethereum wallet has been registered
     */
    override fun register(
        accountName: String,
        domainId: String,
        whitelist: List<String>,
        publicKey: String
    ): Result<String, Exception> {
        return ethFreeRelayProvider.getRelay()
            .flatMap { freeEthWallet ->
                ethRelayProvider.getRelayByAccountId("$accountName@$domainId")
                    .flatMap { assignedRelays ->
                        // check that client hasn't been registered yet
                        if (assignedRelays.isPresent())
                            throw IllegalArgumentException("Client $accountName@$domainId has already been registered with relay: ${assignedRelays.get()}")

                        // register to Ethereum RelayRegistry
                        deployHelper.addRelayToRelayRegistry(
                            ethRegistrationConfig.ethRelayRegistryAddress,
                            freeEthWallet,
                            whitelist
                        )

                        // register relay to Iroha
                        ethereumAccountRegistrator.register(
                            freeEthWallet,
                            whitelist,
                            accountName,
                            domainId,
                            publicKey
                        )
                    }
            }
    }

    /**
     * Return number of free relays.
     */
    override fun getFreeAddressNumber(): Result<Int, Exception> {
        return ethFreeRelayProvider.getRelays()
            .map { freeRelays ->
                freeRelays.size
            }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
