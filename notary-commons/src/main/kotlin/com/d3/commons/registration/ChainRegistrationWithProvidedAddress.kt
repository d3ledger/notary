/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.registration

import com.d3.commons.model.D3ErrorException
import com.d3.commons.notary.IrohaCommand
import com.d3.commons.notary.IrohaTransaction
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.provider.ChainAddressProvider
import mu.KLogging
import java.math.BigInteger

const val REGISTRATION_OPERATION = "Chain user registration"

/**
 * Chain registration in Iroha with provided chain address
 */
class ChainRegistrationWithProvidedAddress(
    private val chainAddressProvider: ChainAddressProvider,
    private val irohaConsumer: IrohaConsumer,
    private val storageAccountId: String,
    private val chainKeyName: String
) {

    init {
        logger.info { "Init $chainKeyName ChainRegistrationWithProvidedAddress with irohaCreator=${irohaConsumer.creator}, storageAccountId=$storageAccountId" }
    }

    /**
     * Register client chain address
     * @param accountId - account that will be assigned a provided chain address
     * @param chainAddress - chain address for iroha client with clientId
     * @return iroha transaction of chain address registration
     */
    fun register(
        accountId: String,
        chainAddress: String,
        time: BigInteger
    ): IrohaTransaction {
        val assignedAddress = chainAddressProvider.getAddressByAccountId(accountId).get()
        if (assignedAddress.isPresent)
            throw D3ErrorException.warning(
                failedOperation = REGISTRATION_OPERATION,
                description = "Client $accountId has already been registered in $chainKeyName"
            )
        // register with relay in Iroha
        return IrohaTransaction(
            irohaConsumer.creator,
            time,
            irohaConsumer.getConsumerQuorum().get(),
            arrayListOf(
                IrohaCommand.CommandSetAccountDetail(
                    accountId,
                    chainKeyName,
                    chainAddress
                ),
                IrohaCommand.CommandSetAccountDetail(
                    storageAccountId,
                    chainAddress,
                    accountId
                )
            )
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
