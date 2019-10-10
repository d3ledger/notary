/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.provider

import com.github.kittinunf.result.Result
import java.util.*

/** Interface that provides relation between deployed chain addresses and iroha accounts */
interface ChainAddressProvider {

    /** Return all addresses in form of (chain address -> Iroha account id) */
    fun getAddresses(): Result<Map<String, String>, Exception>

    /**
     * Get address belonging to [irohaAccountId]
     * @return address or [Optional.empty] if address is absent
     */
    fun getAddressByAccountId(irohaAccountId: String): Result<Optional<String>, Exception>
}
