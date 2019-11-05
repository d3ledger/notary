package com.d3.notifications.debug.dto

import com.dumbster.smtp.SmtpMessage

const val TO_HEADER = "To"
const val FROM_HEADER = "From"
const val SUBJECT_HEADER = "Subject"

/**
 * Data class that represents message from the dumbster SMTP server
 */
data class DumbsterMessage(
    val from: String,
    val to: String,
    val subject: String,
    val message: String
)

/**
 * Maps [SmtpMessage] to [DumbsterMessage]
 * @param mail - message to map
 * @return mapped [DumbsterMessage]
 */
fun mapDumbsterMessage(mail: SmtpMessage) = DumbsterMessage(
    from = mail.getHeaderValue(FROM_HEADER),
    to = mail.getHeaderValue(TO_HEADER),
    subject = mail.getHeaderValue(SUBJECT_HEADER),
    message = parseSMTPMessageBody(mail)
)

/**
 * Takes text from SMTP message
 * @param mail - SMTP message
 * @return plain text with no SMTP-related headers
 */
private fun parseSMTPMessageBody(mail: SmtpMessage): String {
    val message = mail.body
    return message.substring(
        message.indexOf('\n') + 1,
        message.lastIndexOf("------")
    )
}
