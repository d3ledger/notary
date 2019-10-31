/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package notifications.environment

import com.d3.chainadapter.client.ReliableIrohaChainListener
import com.d3.chainadapter.client.createPrettySingleThreadPool
import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.provider.NotaryClientsProvider
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.sidechain.iroha.util.impl.RobustIrohaQueryHelperImpl
import com.d3.commons.util.getRandomString
import com.d3.notifications.NOTIFICATIONS_SERVICE_NAME
import com.d3.notifications.config.PushAPIConfig
import com.d3.notifications.config.SMTPConfig
import com.d3.notifications.init.NotificationInitialization
import com.d3.notifications.provider.D3ClientProvider
import com.d3.notifications.push.PushServiceFactory
import com.d3.notifications.push.WebPushAPIServiceImpl
import com.d3.notifications.rest.DumbsterEndpoint
import com.d3.notifications.service.EmailNotificationService
import com.d3.notifications.service.PushNotificationService
import com.d3.notifications.service.ScalingStrategy
import com.d3.notifications.smtp.SMTPServiceImpl
import com.dumbster.smtp.SimpleSmtpServer
import com.nhaarman.mockitokotlin2.spy
import integration.helper.D3_DOMAIN
import integration.helper.IrohaIntegrationHelperUtil
import integration.helper.NotificationsConfigHelper
import integration.registration.RegistrationServiceTestEnvironment
import jp.co.soramitsu.iroha.java.IrohaAPI
import nl.martijndwars.webpush.PushService
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.Closeable
import java.security.Security

/**
 * Notifications service testing environment
 */
class NotificationsIntegrationTestEnvironment(private val integrationHelper: IrohaIntegrationHelperUtil) :
    Closeable {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    val registrationEnvironment = RegistrationServiceTestEnvironment(integrationHelper)

    private val notificationsConfigHelper = NotificationsConfigHelper(integrationHelper.accountHelper)

    val notificationsConfig =
        notificationsConfigHelper.createNotificationsConfig(registrationEnvironment.registrationConfig)

    val smtpConfig = loadRawLocalConfigs(
        "notifications.smtp",
        SMTPConfig::class.java, "smtp.properties"
    )

    val pushConfig = loadRawLocalConfigs(
        "notifications.push",
        PushAPIConfig::class.java, "push.properties"
    )

    val dumbster = SimpleSmtpServer.start(smtpConfig.port)!!

    val dumbsterEndpoint = DumbsterEndpoint(dumbster, notificationsConfig)

    private val irohaAPI =
        IrohaAPI(notificationsConfig.iroha.hostname, notificationsConfig.iroha.port)

    private val chainListenerExecutorService =
        createPrettySingleThreadPool(NOTIFICATIONS_SERVICE_NAME, "iroha-chain-listener")

    private val irohaChainListener =
        ReliableIrohaChainListener(
            rmqConfig = notificationsConfig.rmq,
            irohaQueue = notificationsConfig.blocksQueue,
            autoAck = false,
            consumerExecutorService = chainListenerExecutorService
        )

    private val notificationsIrohaConsumer =
        IrohaConsumerImpl(integrationHelper.accountHelper.notificationsService, irohaAPI)

    private val notificationsQueryHelper = RobustIrohaQueryHelperImpl(
        IrohaQueryHelperImpl(
            irohaAPI,
            integrationHelper.accountHelper.notificationsService.accountId,
            integrationHelper.accountHelper.notificationsService.keyPair
        ),
        notificationsConfig.irohaQueryTimeoutMls
    )

    private val d3ClientProvider = D3ClientProvider(notificationsQueryHelper)

    private val smtpService = SMTPServiceImpl(smtpConfig)

    private val emailNotificationService =
        EmailNotificationService(smtpService, d3ClientProvider)

    val pushService = spy(
        PushService(pushConfig.vapidPubKeyBase64, pushConfig.vapidPrivKeyBase64, "D3 notifications")
    )

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

    private val notaryClientsProvider =
        NotaryClientsProvider(
            notificationsQueryHelper,
            registrationEnvironment.registrationConfig.clientStorageAccount,
            registrationEnvironment.registrationConfig.registrationCredential.accountId.substringBefore("@")
        )

    private val scalingStrategy = ScalingStrategy(notificationsIrohaConsumer, notificationsConfig)

    val notificationInitialization =
        NotificationInitialization(
            notaryClientsProvider,
            notificationsConfig,
            irohaChainListener,
            scalingStrategy,
            listOf(emailNotificationService, pushNotificationService)
        )

    val withdrawalIrohaConsumer = IrohaConsumerImpl(integrationHelper.accountHelper.btcWithdrawalAccount, irohaAPI)

    // Source account
    val srcClientName = String.getRandomString(9)
    val srcClientKeyPair = ModelUtil.generateKeypair()
    val srcClientId = "$srcClientName@$D3_DOMAIN"
    val srcClientConsumer =
        IrohaConsumerImpl(IrohaCredential(srcClientId, srcClientKeyPair), irohaAPI)

    // Destination account
    val destClientName = String.getRandomString(9)
    val destClientKeyPair = ModelUtil.generateKeypair()
    val destClientId = "$destClientName@$D3_DOMAIN"
    val destClientConsumer =
        IrohaConsumerImpl(IrohaCredential(destClientId, destClientKeyPair), irohaAPI)

    override fun close() {
        registrationEnvironment.close()
        integrationHelper.close()
        irohaAPI.close()
        dumbster.close()
        dumbsterEndpoint.close()
        chainListenerExecutorService.shutdownNow()
    }
}
