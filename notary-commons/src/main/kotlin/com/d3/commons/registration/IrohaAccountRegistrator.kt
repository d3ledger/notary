/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.registration

import com.d3.commons.notary.IrohaCommand
import com.d3.commons.notary.IrohaOrderedBatch
import com.d3.commons.notary.IrohaTransaction
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.consumer.IrohaConverter
import com.d3.commons.sidechain.iroha.util.ModelUtil.getCurrentTime
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import mu.KLogging

open class IrohaAccountRegistrator(
    private val irohaConsumer: IrohaConsumer,
    private val notaryIrohaAccount: String,
    private val currencyName: String
) {

    private val creator = irohaConsumer.creator

    /**
     * Creates new account to Iroha with given address
     * @param currencyAddress - address of crypto currency wallet
     * @param userName - client userName in Iroha
     * @param domain - client domain in Iroha
     * @param pubkey - client's public key
     * @param notaryStorageStrategy - function that defines the way newly created account data will be stored in notary
     * @return address associated with userName
     */
    fun register(
        currencyAddress: String,
        userName: String,
        domain: String,
        pubkey: String,
        notaryStorageStrategy: () -> String
    ): Result<String, Exception> {
        return Result.of {
            createAccountCreationBatch(
                currencyAddress,
                userName,
                domain,
                pubkey,
                notaryStorageStrategy
            )
        }.map { irohaTx ->
            val lst = IrohaConverter.convert(irohaTx)
            irohaConsumer.send(lst).fold({ batchResultMap ->
                if (!isAccountCreationBatchSuccessful(batchResultMap)) {
                    throw IllegalStateException("Cannot create account")
                }
            }, { ex -> throw ex })
        }.map {
            logger.info { "New account $userName@$domain was created" }
            currencyAddress
        }
    }

    /**
     * Creates account creation batch with the following commands
     * - SetAccountDetail on client account with assigned [currencyAddress]
     * - SetAccountDetail on notary node account to mark [currencyAddress] as assigned to the particular user
     * @param currencyAddress - address of crypto currency wallet
     * @param userName - client userName in Iroha
     * @param domain - client domain
     * @param pubkey - client's public key
     * @param notaryStorageStrategy - function that defines the way newly created account data will be stored in notary
     * @return batch
     */
    protected open fun createAccountCreationBatch(
        currencyAddress: String,
        userName: String,
        domain: String,
        pubkey: String,
        notaryStorageStrategy: () -> String
    ): IrohaOrderedBatch {
        val accountId = "$userName@$domain"
        return IrohaOrderedBatch(
            listOf(
                IrohaTransaction(
                    creator,
                    getCurrentTime(),
                    1,
                    arrayListOf(
                        // Set user wallet/address in account detail
                        IrohaCommand.CommandSetAccountDetail(
                            accountId,
                            currencyName,
                            currencyAddress
                        ),
                        // Set wallet/address as occupied by user id
                        IrohaCommand.CommandSetAccountDetail(
                            notaryIrohaAccount,
                            currencyAddress,
                            notaryStorageStrategy()
                        )
                    )
                )
            )
        )
    }

    /**
     * Checks if account creation batch successful
     * @param transactionsResult - map of processed transactions and the indicator if it was successful
     * @return 'true' if all [transactionsResult] (except for first transaction, we can ignore it) are successful
     */
    protected open fun isAccountCreationBatchSuccessful(
        transactionsResult: Map<String, Boolean>
    ): Boolean {
        val count = transactionsResult.count { it.value }
        return if (!transactionsResult.values.first()) {
            count == transactionsResult.size - 1
        } else
            count == transactionsResult.size
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
