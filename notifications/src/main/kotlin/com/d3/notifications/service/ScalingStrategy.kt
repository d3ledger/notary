/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.service

import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.util.isCASError
import com.d3.notifications.config.NotificationsConfig
import com.github.kittinunf.result.Result
import iroha.protocol.BlockOuterClass
import jp.co.soramitsu.iroha.java.Transaction
import org.springframework.stereotype.Component
import java.lang.Exception

/**
 * Notification service scaling strategy
 */
@Component
class ScalingStrategy(
    private val notificationsIrohaConsumer: IrohaConsumer,
    private val notificationsConfig: NotificationsConfig
) {

    /**
     * Checks if the block can be handled
     * @param block - block to check
     * @return true if able to handle, false otherwise
     */
    fun isAbleToHandle(block: BlockOuterClass.Block): Result<Boolean, Exception> {
        val transaction = Transaction.builder(notificationsIrohaConsumer.creator).compareAndSetAccountDetail(
            notificationsIrohaConsumer.creator,
            block.blockV1.payload.height.toString(),
            notificationsConfig.nodeId,
            null
        ).build()
        return notificationsIrohaConsumer.send(transaction)
            .fold({
                return Result.of(true)
            }, { ex ->
                return if (isCASError(ex)) {
                    Result.of(false)
                } else {
                    Result.error(ex)
                }
            })
    }
}
