package notifications

import com.d3.commons.config.loadRawLocalConfigs
import com.d3.notifications.config.NotificationsConfig
import integration.helper.ContainerHelper
import integration.helper.DEFAULT_RMQ_PORT
import integration.helper.KGenericContainer
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationStartupIntegrationTest {

    private val containerHelper = ContainerHelper()
    private val dockerfile = "${containerHelper.userDir}/notifications/build/docker/Dockerfile"
    private val contextFolder = "${containerHelper.userDir}/notifications/build/docker"
    private val balancerRmqContainer =
        KGenericContainer("dolgopolovwork/d3-notification-deduplication-rmq").withExposedPorts(DEFAULT_RMQ_PORT)

    // Create notification service container
    private val notificationServiceContainer = containerHelper.createSoraPluginContainer(contextFolder, dockerfile)

    private val notificationsConfig = loadRawLocalConfigs(
        "notifications",
        NotificationsConfig::class.java, "notifications.properties"
    )

    @BeforeAll
    fun setUp() {
        // Start Iroha
        containerHelper.irohaContainer.start()

        // Start RMQ
        containerHelper.rmqContainer.start()

        // Set RMQ credentials
        balancerRmqContainer
            .withEnv("RABBITMQ_DEFAULT_USER", notificationsConfig.balancerRMQ.username!!)
            .withEnv("RABBITMQ_DEFAULT_PASS", notificationsConfig.balancerRMQ.password!!)

        balancerRmqContainer.start()

        notificationServiceContainer.addEnv(
            "NOTIFICATIONS__IROHA_HOSTNAME",
            containerHelper.irohaContainer.toriiAddress.host
        )
        notificationServiceContainer.addEnv(
            "NOTIFICATIONS__IROHA_PORT",
            containerHelper.irohaContainer.toriiAddress.port.toString()
        )
        notificationServiceContainer.addEnv("NOTIFICATIONS_LOCALRMQ_HOST", "127.0.0.1")
        notificationServiceContainer.addEnv(
            "NOTIFICATIONS_LOCALRMQ_PORT", containerHelper.rmqContainer.getMappedPort(DEFAULT_RMQ_PORT).toString()
        )
        notificationServiceContainer.addEnv("NOTIFICATIONS_BALANCERRMQ_HOST", "127.0.0.1")
        notificationServiceContainer.addEnv(
            "NOTIFICATIONS_BALANCERRMQ_PORT", balancerRmqContainer.getMappedPort(DEFAULT_RMQ_PORT).toString()
        )

        notificationServiceContainer.start()
    }


    @AfterAll
    fun tearDown() {
        containerHelper.close()
        balancerRmqContainer.stop()
        notificationServiceContainer.stop()
    }

    /**
     * @given the notification service container
     * @when the container is started
     * @then it keeps working and its status is healthy
     */
    @Test
    fun testHealthCheck() {
        // Let service work a little
        Thread.sleep(10_000)
        assertTrue(containerHelper.isServiceHealthy(notificationServiceContainer))
    }
}
