/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.config

interface ConfigValidationRule<T> {
    /* Validation rule itself. Must throw IllegalArgumentException in case of validation violations*/
    fun validate(config: T)
}
