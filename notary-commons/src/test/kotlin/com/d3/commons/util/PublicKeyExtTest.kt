/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.util

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.security.PublicKey

class PublicKeyExtTest {

    @Test
    fun toHex() {
        val pubkey = mock<PublicKey> {
            on { this.encoded } doReturn ByteArray(32) { byte -> byte.toByte() }
        }
        Assertions.assertEquals(
            "000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F",
            pubkey.toHexString()
        )
    }
}
