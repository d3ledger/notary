/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.model

import mu.KLogging

/**
 * Exception that represents error in the D3 system
 * @param failedOperation - failed business operation. Bad choice: get account detail, load file. Good choice: Ethereum deposit, User registration
 * @param description - description of a failed business operation.
 * @param severity - level of severity
 * @param errorCause - exception that caused the error
 */
class D3ErrorException(
    val failedOperation: String,
    val description: String,
    val severity: D3ErrorSeverity,
    val errorCause: java.lang.Exception? = null
) : Exception("Failed operation '$failedOperation'. $description. Severity level is $severity", errorCause) {
    companion object {

        /**
         * Creates an instance of D3ErrorException with a severity being set to WARNING
         * @param failedOperation - failed business operation. Bad choice: get account detail, load file. Good choice: Ethereum deposit, User registration
         * @param description - description of a failed business operation.
         * @param errorCause - exception that caused the error
         * @return instance of D3ErrorException
         */
        fun warning(
            failedOperation: String,
            description: String,
            errorCause: java.lang.Exception? = null
        ) = D3ErrorException(failedOperation, description, D3ErrorSeverity.WARNING, errorCause)

        /**
         * Creates an instance of D3ErrorException with a severity being set to FATAL
         * @param failedOperation - failed business operation. Bad choice: get account detail, load file. Good choice: Ethereum deposit, User registration
         * @param description - description of a failed business operation.
         * @param errorCause - exception that caused the error
         * @return instance of D3ErrorException
         */
        fun fatal(
            failedOperation: String,
            description: String,
            errorCause: java.lang.Exception? = null
        ) = D3ErrorException(failedOperation, description, D3ErrorSeverity.FATAL, errorCause)
    }
}

/**
 * Enum class that represents the level of error severity
 */
enum class D3ErrorSeverity {
    /**
     * Acceptable level of severity. Nothing serious. For example, failed user registration.
     */
    WARNING,

    /**
     * The maximum level of severity. Something really serious happened. For example, failed withdrawal.
     */
    FATAL

    //TODO add more levels
}
