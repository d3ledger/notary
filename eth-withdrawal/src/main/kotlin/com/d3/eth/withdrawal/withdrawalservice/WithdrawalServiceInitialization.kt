/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.withdrawal.withdrawalservice

import com.d3.commons.config.EthereumPasswords
import com.d3.commons.config.RMQConfig
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.sidechain.iroha.IrohaChainHandler
import com.d3.commons.sidechain.iroha.ReliableIrohaChainListener
import com.d3.commons.util.createPrettyFixThreadPool
import com.d3.commons.util.createPrettySingleThreadPool
import com.d3.eth.vacuum.RelayVacuumConfig
import com.d3.eth.withdrawal.consumer.EthConsumer
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import jp.co.soramitsu.iroha.java.IrohaAPI
import mu.KLogging

/**
 * @param withdrawalConfig - configuration for withdrawal service
 * @param withdrawalEthereumPasswords - passwords for ethereum withdrawal account
 */
class WithdrawalServiceInitialization(
    private val withdrawalConfig: WithdrawalServiceConfig,
    private val credential: IrohaCredential,
    private val irohaAPI: IrohaAPI,
    private val withdrawalEthereumPasswords: EthereumPasswords,
    private val relayVacuumConfig: RelayVacuumConfig,
    private val rmqConfig: RMQConfig
) {

    /**
     * Init Iroha chain listener
     * @return Observable on Iroha sidechain events
     */
    private fun initIrohaChain(): Result<Observable<SideChainEvent.IrohaEvent>, Exception> {
        logger.info { "Init Iroha chain listener" }
        return ReliableIrohaChainListener(
            rmqConfig,
            withdrawalConfig.ethIrohaWithdrawalQueue,
            createPrettySingleThreadPool(ETH_WITHDRAWAL_SERVICE_NAME, "rmq-consumer")
        ).getBlockObservable()
            .map { observable ->
                observable.flatMapIterable { (block, _) -> IrohaChainHandler().parseBlock(block) }
            }
    }

    /**
     * Init Withdrawal Service
     */
    private fun initWithdrawalService(inputEvents: Observable<SideChainEvent.IrohaEvent>): WithdrawalService {
        return WithdrawalServiceImpl(withdrawalConfig, credential, irohaAPI, inputEvents)
    }

    private fun initEthConsumer(withdrawalService: WithdrawalService): Result<Unit, Exception> {
        logger.info { "Init Ether withdrawal consumer" }

        return Result.of {
            val ethConsumer = EthConsumer(
                withdrawalConfig.ethereum,
                withdrawalEthereumPasswords,
                relayVacuumConfig
            )
            withdrawalService.output()
                .subscribeOn(
                    Schedulers.from(
                        createPrettyFixThreadPool(
                            ETH_WITHDRAWAL_SERVICE_NAME,
                            "event-handler"
                        )
                    )
                )
                .subscribe(
                    { res ->
                        res.map { withdrawalEvents ->
                            withdrawalEvents.map { event ->
                                val transactionReceipt = ethConsumer.consume(event)
                                // TODO: Add subtraction of assets from master account in Iroha in 'else'
                                if (transactionReceipt == null || transactionReceipt.status == FAILED_STATUS) {
                                    withdrawalService.returnIrohaAssets(event)
                                }
                            }
                        }.failure { ex ->
                            logger.error("Cannot consume withdrawal event", ex)
                        }
                        //TODO call ack()
                    }, { ex ->
                        logger.error("Withdrawal observable error", ex)
                    }
                )
            Unit
        }
    }

    fun init(): Result<Unit, Exception> {
        logger.info {
            "Start withdrawal service init with iroha at ${withdrawalConfig.iroha.hostname}:${withdrawalConfig.iroha.port}"
        }
        return initIrohaChain()
            .map { initWithdrawalService(it) }
            .flatMap { initEthConsumer(it) }
            .map { WithdrawalServiceEndpoint(withdrawalConfig.port) }
            .map { Unit }
    }

    /**
     * Logger
     */
    companion object : KLogging() {
        private const val FAILED_STATUS = "0x0"
    }
}
