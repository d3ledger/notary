/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.exception

/**
 * Exception that signals that the failed operation may be repeated later
 */
class RepeatableException(message: String, cause: Throwable) : Exception(message, cause)
