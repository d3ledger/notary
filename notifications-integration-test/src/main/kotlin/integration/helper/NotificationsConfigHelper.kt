/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.helper

import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.registration.NotaryRegistrationConfig
import com.d3.commons.util.getRandomString
import com.d3.notifications.config.NotificationsConfig

class NotificationsConfigHelper(private val accountHelper: IrohaAccountHelper) :
    IrohaConfigHelper() {

    /**
     * Creates notification services config
     */
    fun createNotificationsConfig(registrationConfig: NotaryRegistrationConfig): NotificationsConfig {
        val notificationsConfig = loadRawLocalConfigs(
            "notifications",
            NotificationsConfig::class.java, "notifications.properties"
        )
        return object : NotificationsConfig {
            override val nodeId = String.getRandomString(10)
            override val rmq = notificationsConfig.rmq
            override val blocksQueue = String.getRandomString(10)
            override val irohaQueryTimeoutMls = notificationsConfig.irohaQueryTimeoutMls
            override val registrationServiceAccountName =
                registrationConfig.registrationCredential.accountId.substringBefore("@")
            override val clientStorageAccount = registrationConfig.clientStorageAccount
            override val webPort = notificationsConfig.webPort
            override val withdrawalBillingAccount = notificationsConfig.withdrawalBillingAccount
            override val transferBillingAccount = notificationsConfig.transferBillingAccount
            override val iroha = createIrohaConfig()
            override val notificationCredential =
                accountHelper.createCredentialRawConfig(accountHelper.notaryAccount)
        }
    }
}
