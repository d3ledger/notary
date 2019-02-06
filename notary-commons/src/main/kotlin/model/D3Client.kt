package model

const val D3_CLIENT_EMAIL_KEY = "email"
const val D3_CLIENT_ENABLE_NOTIFICATIONS = "notifications"

/**
 * D3 client data class
 */
data class D3Client(
    val accountId: String,
    val email: String?,
    val enableNotifications: Boolean
) {
    companion object {

        /**
         * Factory function that creates D3 client object using client account details
         * @param name - client accountId
         * @param details - account details taken from Iroha
         * @return d3 client
         */
        fun create(name: String, details: Map<String, String>): D3Client {
            val enableNotifications: Boolean
            details[D3_CLIENT_ENABLE_NOTIFICATIONS].let { notificationSetting ->
                enableNotifications = ("true" == notificationSetting)
            }
            return D3Client(name, details[D3_CLIENT_EMAIL_KEY], enableNotifications)
        }
    }
}
