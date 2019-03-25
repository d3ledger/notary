/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.notifications.smtp

import com.github.kittinunf.result.Result

/**
 * Interface of service that is used to send email messages using SMTP
 */
interface SMTPService {

    /**
     * Sends email message
     * @param from - email of sender
     * @param to - destination email
     * @param subject - subject of email message
     * @param message - message itself
     * @return result of operation
     */
    fun sendMessage(from: String, to: String, subject: String, message: String): Result<Unit, Exception>
}
