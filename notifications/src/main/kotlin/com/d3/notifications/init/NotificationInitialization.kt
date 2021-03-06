/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.init

import com.d3.commons.sidechain.iroha.IrohaChainListener
import com.d3.commons.sidechain.iroha.NOTARY_DOMAIN
import com.d3.commons.sidechain.iroha.util.getTransferCommands
import com.d3.commons.util.createPrettySingleThreadPool
import com.d3.notifications.NOTIFICATIONS_SERVICE_NAME
import com.d3.notifications.service.NotificationService
import com.d3.notifications.service.TransferNotifyEvent
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import io.reactivex.schedulers.Schedulers
import iroha.protocol.Commands
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Notifications initialization service
 */
@Component
class NotificationInitialization(
    @Autowired private val irohaChainListener: IrohaChainListener,
    @Autowired private val notificationServices: List<NotificationService>
) {

    /**
     * Initiates notification service
     * @param onIrohaChainFailure - function that will be called in case of Iroha failure
     */
    fun init(onIrohaChainFailure: () -> Unit) {
        irohaChainListener.getBlockObservable().map { irohaObservable ->
            irohaObservable
                .subscribeOn(
                    Schedulers.from(
                        createPrettySingleThreadPool(
                            NOTIFICATIONS_SERVICE_NAME, "iroha-chain-listener"
                        )
                    )
                )
                .subscribe(
                    { block ->
                        //Get transfer commands from block
                        getTransferCommands(block).forEach { command ->
                            val transferAsset = command.transferAsset
                            // Notify deposit
                            if (isDeposit(transferAsset)) {
                                handleDepositNotification(transferAsset)
                            }
                            // Notify withdrawal
                            else if (isWithdrawal(transferAsset)) {
                                handleWithdrawalEventNotification(transferAsset)
                            }
                        }
                    }, { ex ->
                        logger.error("Error on Iroha subscribe", ex)
                        onIrohaChainFailure()
                    })
        }
    }

    // Checks if deposit event
    private fun isDeposit(transferAsset: Commands.TransferAsset): Boolean {
        return transferAsset.srcAccountId.endsWith("@$NOTARY_DOMAIN")
    }

    // Handles deposit event notification
    private fun handleDepositNotification(transferAsset: Commands.TransferAsset) {
        val transferNotifyEvent = TransferNotifyEvent(
            transferAsset.destAccountId,
            BigDecimal(transferAsset.amount),
            transferAsset.assetId
        )
        logger.info { "Notify deposit $transferNotifyEvent" }
        notificationServices.forEach {
            it.notifyDeposit(
                transferNotifyEvent
            ).failure { ex -> logger.error("Cannot notify deposit", ex) }
        }
    }

    // Checks if withdrawal event
    private fun isWithdrawal(transferAsset: Commands.TransferAsset): Boolean {
        return transferAsset.destAccountId.endsWith("@$NOTARY_DOMAIN")
    }

    // Handles withdrawal event notification
    private fun handleWithdrawalEventNotification(transferAsset: Commands.TransferAsset) {
        val transferNotifyEvent = TransferNotifyEvent(
            transferAsset.srcAccountId,
            BigDecimal(transferAsset.amount),
            transferAsset.assetId
        )
        logger.info { "Notify withdrawal $transferNotifyEvent" }
        notificationServices.forEach {
            it.notifyWithdrawal(
                transferNotifyEvent
            ).failure { ex -> logger.error("Cannot notify withdrawal", ex) }
        }
    }

    /**
     *  Initiates notification service.
     *  This overloaded version does nothing on Iroha failure.
     *  Good for testing purposes.
     */
    fun init() {
        init {}
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
