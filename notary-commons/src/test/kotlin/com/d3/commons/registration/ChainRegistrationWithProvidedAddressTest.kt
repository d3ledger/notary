/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.registration

import com.d3.commons.notary.IrohaCommand
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.provider.ChainAddressProvider
import com.github.kittinunf.result.Result
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChainRegistrationWithProvidedAddressTest {

    private val assignedClientId = "assigned@client"
    private val assignedAddress = "assigned_address"
    private val unassignedClientid = "unassigned@client"
    private val chainAddress = "0xchain_address"
    private val storageAccountId = "storage@notary"
    private val chainId = "chain_id"
    private val time = BigInteger.ZERO

    private val ethAddressProvider = mock<ChainAddressProvider> {
        on {
            getAddressByAccountId(
                eq(assignedClientId)
            )
        } doReturn Result.of { Optional.of(assignedAddress) }


        on {
            getAddressByAccountId(
                eq(unassignedClientid)
            )
        } doReturn Result.of { Optional.empty<String>() }
    }

    private val irohaConsumer = mock<IrohaConsumer> {
        on {
            creator
        } doReturn "creator@notary"
        on {
            getConsumerQuorum()
        } doReturn Result.of { 1 }

    }
    private val registrationStrategy = ChainRegistrationWithProvidedAddress(
        ethAddressProvider,
        irohaConsumer,
        storageAccountId,
        chainId
    )

    /**
     * @given registration strategy
     * @when is called with client id already assigned
     * @then exception is thrown
     */
    @Test
    fun testAlreadyRegistered() {
        Assertions.assertThrows(com.d3.commons.model.D3ErrorException::class.java) {
            registrationStrategy.register(assignedClientId, "any", time)
        }
    }

    /**
     * @given registration strategy
     * @when is called with correct client id and address
     * @then correct iroha tx is returned
     */
    @Test
    fun testRegistration() {
        val tx = registrationStrategy.register(unassignedClientid, chainAddress, time)
        assertEquals(2, tx.commands.count())
        assertTrue { tx.commands[0] is IrohaCommand.CommandSetAccountDetail }

        val cmd0 = tx.commands[0] as IrohaCommand.CommandSetAccountDetail
        assertEquals(unassignedClientid, cmd0.accountId)
        assertEquals(chainId, cmd0.key)
        assertEquals(chainAddress, cmd0.value)

        val cmd1 = tx.commands[1] as IrohaCommand.CommandSetAccountDetail
        assertEquals(storageAccountId, cmd1.accountId)
        assertEquals(chainAddress, cmd1.key)
        assertEquals(unassignedClientid, cmd1.value)
    }
}
