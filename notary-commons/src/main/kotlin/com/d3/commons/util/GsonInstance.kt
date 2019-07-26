/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.util

import com.google.gson.Gson

/**
 * Gson object holder
 */
object GsonInstance {
    private val gson = Gson()

    fun get() = gson
}
