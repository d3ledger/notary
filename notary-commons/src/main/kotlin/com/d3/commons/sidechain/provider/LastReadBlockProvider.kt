/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.provider

import java.math.BigInteger

interface LastReadBlockProvider {
    fun getLastBlockHeight(): BigInteger
    fun saveLastBlockHeight(height: BigInteger)
}
