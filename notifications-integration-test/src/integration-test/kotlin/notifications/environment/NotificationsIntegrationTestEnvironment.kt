/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package notifications.environment

import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.CLIENT_DOMAIN
import com.d3.commons.sidechain.iroha.IrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.util.getRandomString
import com.d3.notifications.init.NotificationInitialization
import com.d3.notifications.provider.D3ClientProvider
import com.d3.notifications.push.PushServiceFactory
import com.d3.notifications.push.WebPushAPIServiceImpl
import com.d3.notifications.rest.DumbsterEndpoint
import com.d3.notifications.service.EmailNotificationService
import com.d3.notifications.service.PushNotificationService
import com.d3.notifications.smtp.SMTPServiceImpl
import com.dumbster.smtp.SimpleSmtpServer
import com.nhaarman.mockitokotlin2.spy
import integration.helper.IrohaIntegrationHelperUtil
import integration.helper.NotificationsConfigHelper
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

    private val notificationsConfigHelper =
        NotificationsConfigHelper(integrationHelper.accountHelper)

    val notificationsConfig = notificationsConfigHelper.createNotificationsConfig()

    val dumbster = SimpleSmtpServer.start(notificationsConfig.smtp.port)!!

    val dumbsterEndpoint = DumbsterEndpoint(dumbster, notificationsConfig)

    private val irohaAPI =
        IrohaAPI(notificationsConfig.iroha.hostname, notificationsConfig.iroha.port)

    private val irohaChainListener =
        IrohaChainListener(irohaAPI, integrationHelper.accountHelper.notaryAccount)

    private val notaryQueryHelper = IrohaQueryHelperImpl(
        irohaAPI,
        integrationHelper.accountHelper.notaryAccount.accountId,
        integrationHelper.accountHelper.notaryAccount.keyPair
    )

    private val d3ClientProvider = D3ClientProvider(notaryQueryHelper)

    private val smtpService = SMTPServiceImpl(notificationsConfig.smtp)

    private val emailNotificationService =
        EmailNotificationService(smtpService, d3ClientProvider)

    val pushService =
        spy(
            PushService(
                notificationsConfig.push.vapidPubKeyBase64,
                notificationsConfig.push.vapidPrivKeyBase64,
                "D3 notifications"
            )
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

    val notificationInitialization =
        NotificationInitialization(
            notificationsConfig,
            irohaChainListener,
            listOf(emailNotificationService, pushNotificationService)
        )

    val withdrawalIrohaConsumer = IrohaConsumerImpl(integrationHelper.accountHelper.btcWithdrawalAccount, irohaAPI)

    // Source account
    val srcClientName = String.getRandomString(9)
    val srcClientKeyPair = ModelUtil.generateKeypair()
    val srcClientId = "$srcClientName@$CLIENT_DOMAIN"
    val srcClientConsumer =
        IrohaConsumerImpl(IrohaCredential(srcClientId, srcClientKeyPair), irohaAPI)

    // Destination account
    val destClientName = String.getRandomString(9)
    val destClientKeyPair = ModelUtil.generateKeypair()
    val destClientId = "$destClientName@$CLIENT_DOMAIN"
    val destClientConsumer =
        IrohaConsumerImpl(IrohaCredential(destClientId, destClientKeyPair), irohaAPI)

    override fun close() {
        integrationHelper.close()
        irohaAPI.close()
        dumbster.close()
        dumbsterEndpoint.close()
    }
}
