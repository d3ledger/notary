/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.helper

import com.d3.commons.config.loadRawLocalConfigs
import com.d3.notifications.config.NotificationsConfig

class NotificationsConfigHelper(private val accountHelper: IrohaAccountHelper) :
    IrohaConfigHelper() {

    /**
     * Creates notification services config
     */
    fun createNotificationsConfig(): NotificationsConfig {
        val notificationsConfig = loadRawLocalConfigs(
            "notifications",
            NotificationsConfig::class.java, "notifications.properties"
        )
        return object : NotificationsConfig {
            override val iroha = createIrohaConfig()
            override val smtp = notificationsConfig.smtp
            override val push = notificationsConfig.push
            override val notaryCredential =
                accountHelper.createCredentialRawConfig(accountHelper.notaryAccount)
        }
    }
}
