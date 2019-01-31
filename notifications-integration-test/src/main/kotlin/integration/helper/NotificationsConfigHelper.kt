package integration.helper

import notifications.config.NotificationsConfig

class NotificationsConfigHelper(private val accountHelper: IrohaAccountHelper) : IrohaConfigHelper() {

    /**
     * Creates notification services config
     */
    fun createNotificationsConfig(): NotificationsConfig {
        return object : NotificationsConfig {
            override val iroha = createIrohaConfig()
            override val smtpConfigPath = "no matter. its not used in tests"
            override val notaryCredential = accountHelper.createCredentialConfig(accountHelper.notaryAccount)
        }
    }
}
