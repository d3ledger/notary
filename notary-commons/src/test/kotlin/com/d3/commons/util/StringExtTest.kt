/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.util

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StringExtTest {

    @Test
    fun hexToAsciiTest() {
        assertEquals("hello hex world", "68656c6c6f2068657820776f726c64".hexToAscii())
    }

    @Test
    fun testHex() {
        val text = "hello hex world"
        assertArrayEquals(text.toByteArray(), String.unHex(String.hex(text.toByteArray())))
    }

    @Test
    fun testIrohaEscape() {
        val text = "\"kek\""
        assertEquals("\\\"kek\\\"", text.irohaEscape())
    }

    @Test
    fun testIrohaUnEscape() {
        val expected = "\"kek\"\nnewline"
        val actual = expected.irohaEscape().irohaUnEscape()
        assertEquals(expected, actual)
    }
}
