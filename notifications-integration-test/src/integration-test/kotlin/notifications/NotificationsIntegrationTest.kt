/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package notifications

import com.d3.commons.util.irohaEscape
import com.d3.commons.util.toHexString
import com.d3.notifications.client.D3_CLIENT_EMAIL_KEY
import com.d3.notifications.client.D3_CLIENT_ENABLE_NOTIFICATIONS
import com.d3.notifications.client.D3_CLIENT_PUSH_SUBSCRIPTION
import com.d3.notifications.service.D3_DEPOSIT_EMAIL_SUBJECT
import com.d3.notifications.service.D3_WITHDRAWAL_EMAIL_SUBJECT
import com.d3.notifications.service.NOTIFICATION_EMAIL
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import com.nhaarman.mockitokotlin2.*
import integration.helper.IrohaIntegrationHelperUtil
import integration.registration.RegistrationServiceTestEnvironment
import notifications.environment.NotificationsIntegrationTestEnvironment
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.math.BigDecimal

const val BTC_ASSET = "btc#bitcoin"
private const val TRANSFER_WAIT_TIME = 3_000L
private const val SRC_USER_EMAIL = "src.user@d3.com"
private const val DEST_USER_EMAIL = "dest.user@d3.com"
private const val SUBSCRIPTION_JSON = "{" +
        "  \"endpoint\": \"https://some.pushservice.com/something-unique\"," +
        "  \"keys\": {" +
        "    \"p256dh\": \"BIPUL12DLfytvTajnryr2PRdAgXS3HGKiLqndGcJGabyhHheJYlNGCeXl1dn18gSJ1WAkAPIxr4gK0_dQds4yiI=\"," +
        "    \"auth\":\"FPssNDTKnInHVndSTdbKFw==\"" +
        "  }" +
        "}"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationsIntegrationTest {

    private val integrationHelper = IrohaIntegrationHelperUtil()

    private val environment = NotificationsIntegrationTestEnvironment(integrationHelper)

    private val registrationEnvironment = RegistrationServiceTestEnvironment(integrationHelper)

    init {
        registrationEnvironment.registrationInitialization.init()

        // This amount is big enough for testing
        val amount = BigDecimal(100)

        // Creating 2 clients
        var res = registrationEnvironment.register(
            environment.srcClientName,
            environment.srcClientKeyPair.public.toHexString()
        )

        kotlin.test.assertEquals(200, res.statusCode)
        res = registrationEnvironment.register(
            environment.destClientName,
            environment.destClientKeyPair.public.toHexString()
        )
        kotlin.test.assertEquals(200, res.statusCode)

        // Setting src client email
        integrationHelper.setAccountDetailWithRespectToBrvs(
            environment.srcClientConsumer,
            environment.srcClientId,
            D3_CLIENT_EMAIL_KEY,
            SRC_USER_EMAIL
        ).failure { ex -> throw ex }

        // Setting src client subscription data
        integrationHelper.setAccountDetailWithRespectToBrvs(
            environment.srcClientConsumer,
            environment.srcClientId,
            D3_CLIENT_PUSH_SUBSCRIPTION,
            SUBSCRIPTION_JSON.irohaEscape()
        ).failure { ex -> throw ex }

        // Setting dest client email
        integrationHelper.setAccountDetailWithRespectToBrvs(
            environment.destClientConsumer,
            environment.destClientId,
            D3_CLIENT_EMAIL_KEY,
            DEST_USER_EMAIL
        ).failure { ex -> throw ex }

        // Enriching notary account
        integrationHelper.addIrohaAssetTo(
            integrationHelper.accountHelper.notaryAccount.accountId,
            BTC_ASSET,
            amount
        )

        // Enriching src account
        integrationHelper.addIrohaAssetTo(
            environment.srcClientId,
            BTC_ASSET,
            amount
        )

        // Run notifications service
        environment.notificationInitialization.init()
    }

    @BeforeEach
    fun resetEnvironment() {
        environment.dumbster.reset()
        reset(environment.pushService)
        val statusLine = mock<StatusLine> {
            on { getStatusCode() } doReturn 200
        }
        val response = mock<HttpResponse> {
            on { getStatusLine() } doReturn statusLine
        }
        doReturn(response).whenever(environment.pushService).send(any())
    }

    @AfterAll
    fun closeEnvironments() {
        registrationEnvironment.close()
        environment.close()
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given D3 client with enabled email notifications and notary account
     * @when notary sends money to D3 client
     * @then D3 client is notified about deposit(both email and push)
     */
    @Test
    fun testNotificationDeposit() {
        val depositValue = BigDecimal(1)

        integrationHelper.setAccountDetailWithRespectToBrvs(
            environment.srcClientConsumer,
            environment.srcClientId,
            D3_CLIENT_ENABLE_NOTIFICATIONS,
            "true"
        ).map {
            val notaryAccount = integrationHelper.accountHelper.notaryAccount
            integrationHelper.transferAssetIrohaFromClient(
                notaryAccount.accountId,
                notaryAccount.keyPair,
                notaryAccount.accountId,
                environment.srcClientId,
                BTC_ASSET,
                "no description",
                depositValue.toPlainString(),
                quorum = 1
            )
        }.map {
            Thread.sleep(TRANSFER_WAIT_TIME)
            val receivedEmails = environment.dumbster.receivedEmails
            assertEquals(1, receivedEmails.size)
            val lastEmail = receivedEmails.last()
            assertEquals(D3_DEPOSIT_EMAIL_SUBJECT, lastEmail.getHeaderValue("Subject"))
            assertEquals(SRC_USER_EMAIL, lastEmail.getHeaderValue("To"))
            assertEquals(NOTIFICATION_EMAIL, lastEmail.getHeaderValue("From"))
            verify(environment.pushService).send(any())
            Unit
        }.failure { ex -> fail(ex) }
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given D3 client with enabled email notifications and notary account
     * @when D3 client sends money to notary account
     * @then D3 client is notified about withdrawal(both email and push)
     */
    @Test
    fun testNotificationWithdrawal() {
        val withdrawalValue = BigDecimal(1)

        integrationHelper.setAccountDetailWithRespectToBrvs(
            environment.srcClientConsumer,
            environment.srcClientId,
            D3_CLIENT_ENABLE_NOTIFICATIONS,
            "true"
        ).map {
            val notaryAccount = integrationHelper.accountHelper.notaryAccount
            integrationHelper.transferAssetIrohaFromClient(
                environment.srcClientId,
                environment.srcClientKeyPair,
                environment.srcClientId,
                notaryAccount.accountId,
                BTC_ASSET,
                "no description",
                withdrawalValue.toPlainString()
            )
        }.map {
            Thread.sleep(TRANSFER_WAIT_TIME)
            val receivedEmails = environment.dumbster.receivedEmails
            assertEquals(1, receivedEmails.size)
            val lastEmail = receivedEmails.last()
            assertEquals(D3_WITHDRAWAL_EMAIL_SUBJECT, lastEmail.getHeaderValue("Subject"))
            assertEquals(SRC_USER_EMAIL, lastEmail.getHeaderValue("To"))
            assertEquals(NOTIFICATION_EMAIL, lastEmail.getHeaderValue("From"))
            verify(environment.pushService).send(any())
            Unit
        }.failure { ex -> fail(ex) }
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given 2 D3 clients with enabled email notifications
     * @when 1st client sends money to 2nd
     * @then None of clients are notified, since it's a simple transfer
     */
    @Test
    fun testNotificationSimpleTransfer() {
        val transferValue = BigDecimal(1)

        integrationHelper.setAccountDetailWithRespectToBrvs(
            environment.srcClientConsumer,
            environment.srcClientId,
            D3_CLIENT_ENABLE_NOTIFICATIONS,
            "true"
        ).map {
            integrationHelper.setAccountDetailWithRespectToBrvs(
                environment.destClientConsumer,
                environment.destClientId,
                D3_CLIENT_ENABLE_NOTIFICATIONS,
                "true"
            )
        }.map {
            integrationHelper.transferAssetIrohaFromClient(
                environment.srcClientId,
                environment.srcClientKeyPair,
                environment.srcClientId,
                environment.destClientId,
                BTC_ASSET,
                "no description",
                transferValue.toPlainString()
            )
        }.map {
            Thread.sleep(TRANSFER_WAIT_TIME)
            assertTrue(environment.dumbster.receivedEmails.isEmpty())
            verify(environment.pushService, never()).send(any())
            Unit
        }.failure { ex -> fail(ex) }
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given D3 client with disabled email notifications and notary account
     * @when notary sends money to D3 client
     * @then D3 client is not notified about deposit via email, but client must be notified via push
     */
    @Test
    fun testNotificationDepositNotEnabledEmail() {
        val depositValue = BigDecimal(1)

        integrationHelper.setAccountDetailWithRespectToBrvs(
            environment.srcClientConsumer,
            environment.srcClientId,
            D3_CLIENT_ENABLE_NOTIFICATIONS,
            "false"
        ).map {
            val notaryAccount = integrationHelper.accountHelper.notaryAccount
            integrationHelper.transferAssetIrohaFromClient(
                notaryAccount.accountId,
                notaryAccount.keyPair,
                notaryAccount.accountId,
                environment.srcClientId,
                BTC_ASSET,
                "no description",
                depositValue.toPlainString(),
                quorum = 1
            )
        }.map {
            Thread.sleep(TRANSFER_WAIT_TIME)
            assertTrue(environment.dumbster.receivedEmails.isEmpty())
            verify(environment.pushService).send(any())
            Unit
        }.failure { ex -> fail(ex) }
    }
}
