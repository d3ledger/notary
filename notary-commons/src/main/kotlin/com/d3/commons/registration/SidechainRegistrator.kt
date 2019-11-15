/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.registration

import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.Transaction
import mu.KLogging

/**
 * Class responsible to bind sidechain address to account in Iroha.
 */
open class SideChainRegistrator(
    private val irohaConsumer: IrohaConsumer,
    private val walletStorageIrohaAccountId: String,
    private val sidechainName: String
) {
    /**
     * Creates new account to Iroha with given address
     * @param currencyAddress - address of crypto currency wallet
     * @param accountId - client account id in Iroha
     * @param notaryStorageStrategy - function that defines the way newly created account data will be stored in notary
     * @return address associated with userName
     */
    fun register(
        currencyAddress: String,
        accountId: String,
        time: Long,
        notaryStorageStrategy: () -> String
    ): Result<String, Exception> {
        return buildTx(currencyAddress, accountId, time, notaryStorageStrategy)
            .flatMap { tx ->
                irohaConsumer.send(tx)
            }.map { hash ->
                logger.info { "$currencyAddress was assigned to account $accountId in iroha tx $hash" }
                currencyAddress
            }
    }

    /**
     * Creates registration transaction
     * @param currencyAddress - address of a specific cryptocurrency to register
     * @param accountId - account id to map [currencyAddress] with
     * @param notaryStorageStrategy - address storage strategy
     */
    fun buildTx(
        currencyAddress: String,
        accountId: String,
        time: Long,
        notaryStorageStrategy: () -> String
    ): Result<Transaction, java.lang.Exception> {
        return irohaConsumer.getConsumerQuorum()
            .map { quorum ->
                Transaction.builder(irohaConsumer.creator)
                    .setAccountDetail(accountId, sidechainName, currencyAddress)
                    .setAccountDetail(
                        walletStorageIrohaAccountId,
                        currencyAddress,
                        notaryStorageStrategy()
                    )
                    .setQuorum(quorum)
                    .setCreatedTime(time)
                    .build()
            }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
