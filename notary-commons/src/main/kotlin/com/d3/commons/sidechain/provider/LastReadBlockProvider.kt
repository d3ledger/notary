/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.provider

interface LastReadBlockProvider {
    fun getLastBlockHeight(): Long
    fun saveLastBlockHeight(height: Long)
}
