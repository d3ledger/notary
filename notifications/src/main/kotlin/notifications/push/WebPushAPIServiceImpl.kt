package notifications.push

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import mu.KLogging
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import nl.martijndwars.webpush.Subscription
import notifications.provider.D3ClientProvider
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import java.security.Security

/**
 * Service that used to send web push notifications using Push API
 */
@Component
class WebPushAPIServiceImpl(
    @Autowired private val d3ClientProvider: D3ClientProvider,
    @Autowired private val pushServiceFactory: PushServiceFactory
) : WebPushAPIService {

    override fun push(accountId: String, message: String): Result<Unit, Exception> {
        return d3ClientProvider.getClient(accountId)
            .map { d3Client ->
                if (d3Client.subscription == null)
                    return@map Result.of {
                        logger.warn {
                            "Cannot send push notification. Client $accountId has no push subscription data"
                        }
                        Unit
                    }
                sendPushMessage(d3Client.subscription, message)
            }.map {
                logger.info { "Push message '$message' has been successfully sent to $accountId" }
            }
    }

    // Sends push message to Push Service
    private fun sendPushMessage(sub: Subscription, message: String) {
        Security.addProvider(BouncyCastleProvider())
        // Create a notification with the endpoint, userPublicKey from the subscription and a custom payload
        val notification = Notification(
            sub.endpoint,
            sub.keys.p256dh,
            sub.keys.auth,
            message.toByteArray(Charset.defaultCharset())
        )
        // Instantiate the push service, no need to use an API key for Push API
        val pushService = pushServiceFactory.create()
        // Send the notification
        pushService.send(notification)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}

interface PushServiceFactory {
    fun create(): PushService
}
