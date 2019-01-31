package notifications.environment

import com.nhaarman.mockito_kotlin.mock
import integration.helper.IrohaIntegrationHelperUtil
import integration.helper.NotificationsConfigHelper
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import model.IrohaCredential
import notifications.init.NotificationInitialization
import notifications.service.EmailNotificationService
import notifications.smtp.SMTPService
import provider.D3ClientProvider
import sidechain.iroha.CLIENT_DOMAIN
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import util.getRandomString

/**
 * Notifications service testing environment
 */
class NotificationsIntegrationTestEnvironment(private val integrationHelper: IrohaIntegrationHelperUtil) {

    private val notificationsConfigHelper = NotificationsConfigHelper(integrationHelper.accountHelper)

    private val notificationsConfig = notificationsConfigHelper.createNotificationsConfig()

    private val irohaAPI = IrohaAPI(notificationsConfig.iroha.hostname, notificationsConfig.iroha.port)

    private val irohaChainListener = IrohaChainListener(irohaAPI, integrationHelper.accountHelper.notaryAccount)

    private val notaryQueryAPI = QueryAPI(
        irohaAPI,
        integrationHelper.accountHelper.notaryAccount.accountId,
        integrationHelper.accountHelper.notaryAccount.keyPair
    )

    private val d3ClientProvider = D3ClientProvider(notaryQueryAPI)

    val smtpService = mock<SMTPService>()

    private val notificationService = EmailNotificationService(smtpService, d3ClientProvider)

    val notificationInitialization = NotificationInitialization(irohaChainListener, notificationService)

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

}
