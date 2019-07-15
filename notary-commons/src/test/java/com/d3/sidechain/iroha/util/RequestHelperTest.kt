/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.sidechain.iroha.util

import com.d3.commons.sidechain.iroha.util.getTransferToAccountCommands
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import iroha.protocol.BlockOuterClass
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import kotlin.test.assertTrue

class RequestHelperTest {

    /**
     * @given empty Iroha block
     * @when getTransferToAccountCommands is called
     * @then empty list returned
     */
    @Test
    fun getTransferToAccountCommandsEmptyBlockTest() {
        val block =
            mock<BlockOuterClass.BlockOrBuilder>(defaultAnswer = Mockito.RETURNS_DEEP_STUBS) {
                on { blockV1OrBuilder.payloadOrBuilder.transactionsList } doReturn emptyList()
            }
        val accountId = "test@test"
        val res = getTransferToAccountCommands(block, accountId)
        assertTrue { res.isEmpty() }
    }
}
