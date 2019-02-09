package notifications.smtp

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import mu.KLogging
import notifications.config.SMTPConfig
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

/**
 * Service that is used to send email messages using SMTP
 */
class SMTPServiceImpl(private val smtpConfig: SMTPConfig) : SMTPService {

    private val properties = Properties()

    init {
        properties["mail.smtp.auth"] = true
        properties["mail.smtp.host"] = smtpConfig.host
        properties["mail.smtp.port"] = smtpConfig.port
        properties["mail.smtp.ssl.trust"] = smtpConfig.host
        properties["mail.smtp.starttls.enable"] = true
    }

    override fun sendMessage(from: String, to: String, subject: String, message: String): Result<Unit, Exception> {
        return Result.of {
            Session.getInstance(properties, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(smtpConfig.userName, smtpConfig.password)
                }
            })
        }.map { session ->
            val mimeMessage = MimeMessage(session)
            mimeMessage.setFrom(InternetAddress(from))
            mimeMessage.setRecipients(
                Message.RecipientType.TO, InternetAddress.parse(to)
            )
            mimeMessage.subject = subject
            val mimeBodyPart = MimeBodyPart()
            mimeBodyPart.setContent(message, "text/html")
            val multipart = MimeMultipart()
            multipart.addBodyPart(mimeBodyPart)
            mimeMessage.setContent(multipart)
            Transport.send(mimeMessage)
        }.map {
            logger.info { "Message to $to has been successfully sent" }
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
