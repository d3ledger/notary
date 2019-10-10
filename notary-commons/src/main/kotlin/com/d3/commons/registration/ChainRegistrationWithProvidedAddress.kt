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

const val REGISTRATION_OPERATION = "Ethereum user registration"

/**
 * Ethereum registration in Iroha with provided ethereum address
 */
class ChainRegistrationWithProvidedAddress(
    private val ethAddressProvider: ChainAddressProvider,
    private val irohaConsumer: IrohaConsumer,
    private val storageAccountId: String,
    private val chainKeyName: String
) {

    init {
        logger.info { "Init $chainKeyName ChainRegistrationWithProvidedAddress with irohaCreator=${irohaConsumer.creator}, storageAccountId=$storageAccountId" }
    }

    private val ethereumAccountRegistrator = SideChainRegistrator(
        irohaConsumer,
        storageAccountId,
        chainKeyName
    )

    /**
     * @param accountId - account that will be assigned a provided ethereum address
     * @param ethAddress - ethereum address for iroha client with clientId
     * @return transaction of
     */
    fun register(
        accountId: String,
        ethAddress: String,
        time: BigInteger
    ): IrohaTransaction {
        val assignedAddress = ethAddressProvider.getAddressByAccountId(accountId).get()
        if (assignedAddress.isPresent)
            throw D3ErrorException.warning(
                failedOperation = REGISTRATION_OPERATION,
                description = "Client $accountId has already been registered with address: ${assignedAddress.get()}"
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
                    ethAddress
                ),
                IrohaCommand.CommandSetAccountDetail(
                    storageAccountId,
                    ethAddress,
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
