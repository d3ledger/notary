package notifications.environment

import com.d3.notifications.config.PushAPIConfig
import com.d3.notifications.config.SMTPConfig
import com.d3.notifications.init.NotificationInitialization
import com.d3.notifications.provider.D3ClientProvider
import com.d3.notifications.push.PushServiceFactory
import com.d3.notifications.push.WebPushAPIServiceImpl
import com.d3.notifications.service.EmailNotificationService
import com.d3.notifications.service.PushNotificationService
import com.d3.notifications.smtp.SMTPServiceImpl
import com.dumbster.smtp.SimpleSmtpServer
import com.nhaarman.mockito_kotlin.spy
import com.d3.commons.config.getConfigFolder
import com.d3.commons.config.loadRawConfigs
import integration.helper.IrohaIntegrationHelperUtil
import integration.helper.NotificationsConfigHelper
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import com.d3.commons.model.IrohaCredential
import nl.martijndwars.webpush.PushService
import org.bouncycastle.jce.provider.BouncyCastleProvider
import com.d3.commons.sidechain.iroha.CLIENT_DOMAIN
import com.d3.commons.sidechain.iroha.IrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomString
import java.io.Closeable
import java.io.File
import java.security.Security

/**
 * Notifications service testing environment
 */
class NotificationsIntegrationTestEnvironment(private val integrationHelper: IrohaIntegrationHelperUtil) : Closeable {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    private val notificationsConfigHelper = NotificationsConfigHelper(integrationHelper.accountHelper)

    private val notificationsConfig = notificationsConfigHelper.createNotificationsConfig()

    private val smtpConfig = loadRawConfigs(
        "smtp",
        SMTPConfig::class.java,
        getConfigFolder() + File.separator + notificationsConfig.smtpConfigPath
    )

    private val pushAPIConfig = loadRawConfigs(
        "push",
        PushAPIConfig::class.java,
        getConfigFolder() + File.separator + notificationsConfig.pushApiConfigPath
    )
    val dumbster = SimpleSmtpServer.start(smtpConfig.port)!!

    private val irohaAPI = IrohaAPI(notificationsConfig.iroha.hostname, notificationsConfig.iroha.port)

    private val irohaChainListener = IrohaChainListener(irohaAPI, integrationHelper.accountHelper.notaryAccount)

    private val notaryQueryAPI = QueryAPI(
        irohaAPI,
        integrationHelper.accountHelper.notaryAccount.accountId,
        integrationHelper.accountHelper.notaryAccount.keyPair
    )

    private val d3ClientProvider = D3ClientProvider(notaryQueryAPI)

    private val smtpService = SMTPServiceImpl(smtpConfig)

    private val emailNotificationService =
        EmailNotificationService(smtpService, d3ClientProvider)

    val pushService =
        spy(PushService(pushAPIConfig.vapidPubKeyBase64, pushAPIConfig.vapidPrivKeyBase64, "D3 notifications"))

    private val pushServiceFactory = object : PushServiceFactory {
        override fun create() = pushService
    }

    private val pushNotificationService =
        PushNotificationService(
            WebPushAPIServiceImpl(
                d3ClientProvider,
                pushServiceFactory
            )
        )

    val notificationInitialization =
        NotificationInitialization(
            irohaChainListener,
            listOf(emailNotificationService, pushNotificationService)
        )

    // Source account
    val srcClientName = String.getRandomString(9)
    val srcClientKeyPair = ModelUtil.generateKeypair()
    val srcClientId = "$srcClientName@$CLIENT_DOMAIN"
    val srcClientConsumer = IrohaConsumerImpl(IrohaCredential(srcClientId, srcClientKeyPair), irohaAPI)

    // Destination account
    val destClientName = String.getRandomString(9)
    val destClientKeyPair = ModelUtil.generateKeypair()
    val destClientId = "$destClientName@$CLIENT_DOMAIN"
    val destClientConsumer = IrohaConsumerImpl(IrohaCredential(destClientId, destClientKeyPair), irohaAPI)

    override fun close() {
        integrationHelper.close()
        irohaAPI.close()
        dumbster.close()
    }
}
