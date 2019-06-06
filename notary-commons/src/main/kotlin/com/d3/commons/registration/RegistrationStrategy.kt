/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.registration

import com.github.kittinunf.result.Result

/**
 * Strategy for registration of a new client
 */
interface RegistrationStrategy {

    /**
     * Register new client
     */
    fun register(
        accountName: String,
        domainId: String,
        publicKey: String
    ): Result<String, Exception>

    /**
     * Get a number of free addresses for registration
     */
    fun getFreeAddressNumber(): Result<Int, Exception>
}
