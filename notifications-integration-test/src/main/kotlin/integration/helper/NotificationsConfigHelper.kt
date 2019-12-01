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
            override val ethWithdrawalProofSetter = accountHelper.ethWithdrawalProofSetter.accountId
            // No matter what port. It's not used in integration tests
            override val healthCheckPort = 12345
            override val ethWithdrawalAccount = accountHelper.ethWithdrawalAccount.accountId
            override val ethDepositAccount = accountHelper.notaryAccount.accountId
            override val ethRegistrationServiceAccount = accountHelper.ethRegistrationAccount.accountId
            override val rmq = notificationsConfig.rmq
            override val blocksQueue = String.getRandomString(10)
            override val irohaQueryTimeoutMls = notificationsConfig.irohaQueryTimeoutMls
            override val registrationServiceAccountName =
                registrationConfig.registrationCredential.accountId.substringBefore("@")
            override val clientStorageAccount = registrationConfig.clientStorageAccount
            override val withdrawalBillingAccount = notificationsConfig.withdrawalBillingAccount
            override val transferBillingAccount = notificationsConfig.transferBillingAccount
            override val iroha = createIrohaConfig()
            override val notaryCredential =
                accountHelper.createCredentialRawConfig(accountHelper.notaryAccount)
        }
    }

}
