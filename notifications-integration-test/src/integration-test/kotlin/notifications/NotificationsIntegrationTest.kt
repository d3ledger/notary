/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package notifications

import com.d3.commons.registration.FAILED_REGISTRATION_KEY
import com.d3.commons.service.WithdrawalFinalizationDetails
import com.d3.commons.service.WithdrawalFinalizer
import com.d3.commons.sidechain.iroha.FEE_ROLLBACK_DESCRIPTION
import com.d3.commons.sidechain.iroha.ROLLBACK_DESCRIPTION
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.util.GsonInstance
import com.d3.commons.util.irohaEscape
import com.d3.commons.util.toHexString
import com.d3.notifications.client.D3_CLIENT_EMAIL_KEY
import com.d3.notifications.client.D3_CLIENT_ENABLE_NOTIFICATIONS
import com.d3.notifications.client.D3_CLIENT_PUSH_SUBSCRIPTION
import com.d3.notifications.debug.dto.DumbsterMessage
import com.d3.notifications.event.*
import com.d3.notifications.init.BTC_WALLET
import com.d3.notifications.init.ETH_WALLET
import com.d3.notifications.provider.EthWithdrawalProof
import com.d3.notifications.provider.VRSSignature
import com.d3.notifications.service.*
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import com.google.gson.reflect.TypeToken
import com.nhaarman.mockitokotlin2.*
import integration.helper.D3_DOMAIN
import integration.helper.IrohaIntegrationHelperUtil
import jp.co.soramitsu.iroha.java.Transaction
import notifications.environment.NotificationsIntegrationTestEnvironment
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger

private const val WAIT_TIME = 5_000L
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

    private val gson = GsonInstance.get()

    private val integrationHelper = IrohaIntegrationHelperUtil()

    private val environment = NotificationsIntegrationTestEnvironment(integrationHelper)

    init {
        environment.registrationEnvironment.registrationInitialization.init()
        // This amount is big enough for testing
        val amount = BigDecimal(100)

        // Creating 2 clients
        var res = environment.registrationEnvironment.register(
            environment.srcClientName,
            environment.srcClientKeyPair.public.toHexString()
        )

        kotlin.test.assertEquals(200, res.statusCode)
        res = environment.registrationEnvironment.register(
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

        // Setting dest client subscription data
        integrationHelper.setAccountDetailWithRespectToBrvs(
            environment.destClientConsumer,
            environment.destClientId,
            D3_CLIENT_PUSH_SUBSCRIPTION,
            SUBSCRIPTION_JSON.irohaEscape()
        ).failure { ex -> throw ex }

        // Enriching notary account
        integrationHelper.addIrohaAssetTo(
            integrationHelper.accountHelper.notaryAccount.accountId,
            ETH_ASSET_ID,
            amount
        )

        // Enriching withdrawal account
        integrationHelper.addIrohaAssetTo(
            integrationHelper.accountHelper.ethWithdrawalAccount.accountId,
            ETH_ASSET_ID,
            amount
        )

        // Enriching src account
        integrationHelper.addIrohaAssetTo(
            environment.srcClientId,
            ETH_ASSET_ID,
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
        val from = "0x123"
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
                ETH_ASSET_ID,
                from,
                depositValue.toPlainString(),
                quorum = 1
            )
        }.map {
            Thread.sleep(WAIT_TIME)
            val receivedEmails = getAllMails()
            assertEquals(1, receivedEmails.size)
            val lastEmail = receivedEmails.last()
            assertEquals(D3_DEPOSIT_EMAIL_SUBJECT, lastEmail.subject)
            assertEquals(SRC_USER_EMAIL, lastEmail.to)
            assertEquals(NOTIFICATION_EMAIL, lastEmail.from)
            assertTrue(lastEmail.message.contains("from $from"))
            verify(environment.pushService).send(any())
            val soraEvent = getLastSoraEvent(SoraURI.DEPOSIT_URI) as SoraDepositEvent
            assertEquals(environment.srcClientId, soraEvent.accountIdToNotify)
            assertEquals(depositValue, soraEvent.amount)
            assertEquals(ETH_ASSET_ID, soraEvent.assetName)
            assertNotNull(soraEvent.id)
            assertNotNull(soraEvent.time)
            Unit
        }.failure { ex -> fail(ex) }
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given D3 client with enabled email notifications and Ethereum registration service
     * @when Ethereum registration service registers D3 client in the Ethereum subsystem
     * @then D3 client is notified about registration(both email and push)
     */
    @Test
    fun testNotificationEthRegistration() {
        val ethAddress = "abc"
        integrationHelper.setAccountDetailWithRespectToBrvs(
            environment.srcClientConsumer,
            environment.srcClientId,
            D3_CLIENT_ENABLE_NOTIFICATIONS,
            "true"
        ).map {
            val ethRegistrationAccount = integrationHelper.accountHelper.ethRegistrationAccount
            integrationHelper.setAccountDetail(
                IrohaConsumerImpl(ethRegistrationAccount, integrationHelper.irohaAPI),
                environment.srcClientConsumer.creator,
                ETH_WALLET,
                ethAddress
            )
        }.map {
            Thread.sleep(WAIT_TIME)
            val receivedEmails = getAllMails()
            assertEquals(1, receivedEmails.size)
            val lastEmail = receivedEmails.last()
            assertEquals(D3_REGISTRATION_SUBJECT, lastEmail.subject)
            assertEquals(SRC_USER_EMAIL, lastEmail.to)
            assertEquals(NOTIFICATION_EMAIL, lastEmail.from)
            verify(environment.pushService).send(any())
            assertTrue(lastEmail.message.contains(ethAddress))
            assertTrue(lastEmail.message.contains(RegistrationEventSubsystem.ETH.toString()))
            val soraEvent = getLastSoraEvent(SoraURI.REGISTRATION_URI) as SoraRegistrationEvent
            assertEquals(RegistrationEventSubsystem.ETH.name, soraEvent.subsystem)
            assertEquals(environment.srcClientConsumer.creator, soraEvent.accountIdToNotify)
            assertEquals(ethAddress, soraEvent.address)
            assertNotNull(soraEvent.id)
            assertNotNull(soraEvent.time)
            Unit
        }.failure { ex -> fail(ex) }
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given D3 client with enabled email notifications and Ethereum registration service
     * @when Ethereum registration service fails to register D3 client in the Ethereum subsystem
     * @then D3 client is notified about failed registration(both email and push)
     */
    @Test
    fun testNotificationFailedEthRegistration() {
        integrationHelper.setAccountDetailWithRespectToBrvs(
            environment.srcClientConsumer,
            environment.srcClientId,
            D3_CLIENT_ENABLE_NOTIFICATIONS,
            "true"
        ).map {
            val ethRegistrationAccount = integrationHelper.accountHelper.ethRegistrationAccount
            integrationHelper.setAccountDetail(
                IrohaConsumerImpl(ethRegistrationAccount, integrationHelper.irohaAPI),
                environment.srcClientConsumer.creator,
                FAILED_REGISTRATION_KEY, ""
            )
        }.map {
            Thread.sleep(WAIT_TIME)
            val receivedEmails = getAllMails()
            assertEquals(1, receivedEmails.size)
            val lastEmail = receivedEmails.last()
            assertEquals(D3_FAILED_REGISTRATION_SUBJECT, lastEmail.subject)
            assertEquals(SRC_USER_EMAIL, lastEmail.to)
            assertEquals(NOTIFICATION_EMAIL, lastEmail.from)
            verify(environment.pushService).send(any())
            assertTrue(lastEmail.message.contains(RegistrationEventSubsystem.ETH.toString()))
            val soraEvent = getLastSoraEvent(SoraURI.FAILED_REGISTRATION_URI) as SoraFailedRegistrationEvent
            assertEquals(RegistrationEventSubsystem.ETH.name, soraEvent.subsystem)
            assertEquals(environment.srcClientConsumer.creator, soraEvent.accountIdToNotify)
            assertNotNull(soraEvent.id)
            assertNotNull(soraEvent.time)
            Unit
        }.failure { ex -> fail(ex) }
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given D3 client with enabled email notifications and Bitcoin registration service
     * @when Bitcoin registration service registers D3 client in the Bitcoin subsystem
     * @then D3 client is notified about registration(both email and push)
     */
    @Test
    fun testNotificationBtcRegistration() {
        val btcAddress = "xyz"
        integrationHelper.setAccountDetailWithRespectToBrvs(
            environment.srcClientConsumer,
            environment.srcClientId,
            D3_CLIENT_ENABLE_NOTIFICATIONS,
            "true"
        ).map {
            val btcRegistrationAccount = integrationHelper.accountHelper.btcRegistrationAccount
            integrationHelper.setAccountDetail(
                IrohaConsumerImpl(btcRegistrationAccount, integrationHelper.irohaAPI),
                environment.srcClientConsumer.creator,
                BTC_WALLET,
                btcAddress
            )
        }.map {
            Thread.sleep(WAIT_TIME)
            val receivedEmails = getAllMails()
            assertEquals(1, receivedEmails.size)
            val lastEmail = receivedEmails.last()
            assertEquals(D3_REGISTRATION_SUBJECT, lastEmail.subject)
            assertEquals(SRC_USER_EMAIL, lastEmail.to)
            assertEquals(NOTIFICATION_EMAIL, lastEmail.from)
            verify(environment.pushService).send(any())
            assertTrue(lastEmail.message.contains(btcAddress))
            assertTrue(lastEmail.message.contains(RegistrationEventSubsystem.BTC.toString()))
            Unit
        }.failure { ex -> fail(ex) }
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given D3 client with enabled email notifications and notary account
     * @when D3 client finalizes withdrawal operation with fee
     * @then D3 client is notified about withdrawal(both email and push) and fee
     */
    @Test
    fun testNotificationWithdrawalWithFee() {
        val withdrawalValue = BigDecimal(1)
        val fee = BigDecimal("0.1")
        val destAddress = "0x123"
        integrationHelper.setAccountDetailWithRespectToBrvs(
            environment.srcClientConsumer,
            environment.srcClientId,
            D3_CLIENT_ENABLE_NOTIFICATIONS,
            "true"
        ).flatMap {
            val withdrawalFinalizer = WithdrawalFinalizer(
                environment.withdrawalIrohaConsumer,
                "withdrawal_billing@$D3_DOMAIN"
            )
            val withdrawalFinalizationDetails = WithdrawalFinalizationDetails(
                withdrawalValue,
                ETH_ASSET_ID,
                fee,
                ETH_ASSET_ID,
                environment.srcClientId,
                System.currentTimeMillis(),
                destAddress
            )
            withdrawalFinalizer.finalize(withdrawalFinalizationDetails)
        }.map {
            Thread.sleep(WAIT_TIME)
            val receivedEmails = getAllMails()
            assertEquals(1, receivedEmails.size)
            val lastEmail = receivedEmails.last()
            assertEquals(D3_WITHDRAWAL_EMAIL_SUBJECT, lastEmail.subject)
            assertEquals(SRC_USER_EMAIL, lastEmail.to)
            assertEquals(NOTIFICATION_EMAIL, lastEmail.from)
            assertTrue(lastEmail.message.contains("Fee is $fee $ETH_ASSET_ID"))
            assertTrue(lastEmail.message.contains("to $destAddress"))
            verify(environment.pushService).send(any())
            val soraEvent = getLastSoraEvent(SoraURI.WITHDRAWAL_URI) as SoraWithdrawalEvent
            assertEquals(environment.srcClientId, soraEvent.accountIdToNotify)
            assertEquals(destAddress, soraEvent.to)
            assertEquals(withdrawalValue, soraEvent.amount)
            assertEquals(ETH_ASSET_ID, soraEvent.assetName)
            assertEquals(fee, soraEvent.fee!!.amount)
            assertEquals(ETH_ASSET_ID, soraEvent.fee!!.assetName)
            assertNotNull(soraEvent.id)
            assertNotNull(soraEvent.time)
            assertNull(soraEvent.sideChainFee)
            Unit
        }.failure { ex -> fail(ex) }
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given D3 client with enabled email notifications and notary account
     * @when D3 client finalizes withdrawal operation with regular fee and sidechain fee
     * @then D3 client is notified about withdrawal(both email and push) and fee
     */
    @Test
    fun testNotificationWithdrawalWithRegularFeeAndSideChainFee() {
        val withdrawalValue = BigDecimal(1)
        val fee = BigDecimal("0.1")
        val sideChainFee = BigDecimal("0.01")
        val destAddress = "0x123"
        integrationHelper.setAccountDetailWithRespectToBrvs(
            environment.srcClientConsumer,
            environment.srcClientId,
            D3_CLIENT_ENABLE_NOTIFICATIONS,
            "true"
        ).flatMap {
            val withdrawalFinalizer = WithdrawalFinalizer(
                environment.withdrawalIrohaConsumer, "withdrawal_billing@$D3_DOMAIN"
            )
            val withdrawalFinalizationDetails = WithdrawalFinalizationDetails(
                withdrawalValue,
                ETH_ASSET_ID,
                fee,
                ETH_ASSET_ID,
                environment.srcClientId,
                System.currentTimeMillis(),
                destAddress,
                sideChainFee
            )
            withdrawalFinalizer.finalize(withdrawalFinalizationDetails)
        }.map {
            Thread.sleep(WAIT_TIME)
            val receivedEmails = getAllMails()
            assertEquals(1, receivedEmails.size)
            val lastEmail = receivedEmails.last()
            assertEquals(D3_WITHDRAWAL_EMAIL_SUBJECT, lastEmail.subject)
            assertEquals(SRC_USER_EMAIL, lastEmail.to)
            assertEquals(NOTIFICATION_EMAIL, lastEmail.from)
            assertTrue(lastEmail.message.contains("Fee is $fee $ETH_ASSET_ID"))
            assertTrue(lastEmail.message.contains("to $destAddress"))
            verify(environment.pushService).send(any())
            val soraEvent = getLastSoraEvent(SoraURI.WITHDRAWAL_URI) as SoraWithdrawalEvent
            assertEquals(environment.srcClientId, soraEvent.accountIdToNotify)
            assertEquals(destAddress, soraEvent.to)
            assertEquals(withdrawalValue, soraEvent.amount)
            assertEquals(ETH_ASSET_ID, soraEvent.assetName)
            assertEquals(fee, soraEvent.fee!!.amount)
            assertEquals(ETH_ASSET_ID, soraEvent.fee!!.assetName)
            assertNotNull(soraEvent.id)
            assertNotNull(soraEvent.time)
            assertEquals(sideChainFee, soraEvent.sideChainFee)
            Unit
        }.failure { ex -> fail(ex) }
    }


    /**
     * Note: Iroha must be deployed to pass the test.
     * @given D3 client with enabled email notifications and notary account
     * @when D3 client finalizes withdrawal operation with no fee
     * @then D3 client is notified about withdrawal(both email and push)
     */
    @Test
    fun testNotificationWithdrawalNoFee() {
        val withdrawalValue = BigDecimal(1)
        val fee = BigDecimal.ZERO
        val destAddress = "0x123"
        integrationHelper.setAccountDetailWithRespectToBrvs(
            environment.srcClientConsumer,
            environment.srcClientId,
            D3_CLIENT_ENABLE_NOTIFICATIONS,
            "true"
        ).flatMap {
            val withdrawalFinalizer = WithdrawalFinalizer(
                environment.withdrawalIrohaConsumer,
                "withdrawal_billing@$D3_DOMAIN"
            )
            val withdrawalFinalizationDetails = WithdrawalFinalizationDetails(
                withdrawalValue,
                ETH_ASSET_ID,
                fee,
                ETH_ASSET_ID,
                environment.srcClientId,
                System.currentTimeMillis(),
                destAddress
            )
            withdrawalFinalizer.finalize(withdrawalFinalizationDetails)
        }.map {
            Thread.sleep(WAIT_TIME)
            val receivedEmails = getAllMails()
            assertEquals(1, receivedEmails.size)
            val lastEmail = receivedEmails.last()
            assertEquals(D3_WITHDRAWAL_EMAIL_SUBJECT, lastEmail.subject)
            assertEquals(SRC_USER_EMAIL, lastEmail.to)
            assertEquals(NOTIFICATION_EMAIL, lastEmail.from)
            assertFalse(lastEmail.message.contains("Fee is"))
            assertTrue(lastEmail.message.contains("to $destAddress"))
            verify(environment.pushService).send(any())
            val soraEvent = getLastSoraEvent(SoraURI.WITHDRAWAL_URI) as SoraWithdrawalEvent
            assertEquals(environment.srcClientId, soraEvent.accountIdToNotify)
            assertEquals(destAddress, soraEvent.to)
            assertEquals(withdrawalValue, soraEvent.amount)
            assertEquals(ETH_ASSET_ID, soraEvent.assetName)
            assertNull(soraEvent.fee)
            assertNotNull(soraEvent.id)
            assertNotNull(soraEvent.time)
            assertNull(soraEvent.sideChainFee)
            Unit
        }.failure { ex -> fail(ex) }
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given D3 client with enabled email notifications and notary account
     * @when notary account rollbacks money to D3 client
     * @then D3 client is notified about rollback(both email and push)
     */
    @Test
    fun testNotificationRollbackNoFee() {
        val rollbackValue = BigDecimal(1)
        integrationHelper.setAccountDetailWithRespectToBrvs(
            environment.srcClientConsumer,
            environment.srcClientId,
            D3_CLIENT_ENABLE_NOTIFICATIONS,
            "true"
        ).map {
            val withdrawalAccount = integrationHelper.accountHelper.ethWithdrawalAccount
            integrationHelper.transferAssetIrohaFromClient(
                withdrawalAccount.accountId,
                withdrawalAccount.keyPair,
                withdrawalAccount.accountId,
                environment.srcClientId,
                ETH_ASSET_ID,
                ROLLBACK_DESCRIPTION,
                rollbackValue.toPlainString(),
                quorum = 1
            )
        }.map {
            Thread.sleep(WAIT_TIME)
            val receivedEmails = getAllMails()
            assertEquals(1, receivedEmails.size)
            val lastEmail = receivedEmails.last()
            assertEquals(D3_ROLLBACK_SUBJECT, lastEmail.subject)
            assertEquals(SRC_USER_EMAIL, lastEmail.to)
            assertEquals(NOTIFICATION_EMAIL, lastEmail.from)
            verify(environment.pushService).send(any())
            Unit
        }.failure { ex -> fail(ex) }
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given D3 client with enabled email notifications and notary account
     * @when notary account rollbacks money+fee to D3 client
     * @then D3 client is notified about rollback+fee(both email and push)
     */
    @Test
    fun testNotificationRollbackWithFee() {
        val rollbackValue = BigDecimal(1)
        val rollbackFeeValue = BigDecimal("0.1")
        integrationHelper.setAccountDetailWithRespectToBrvs(
            environment.srcClientConsumer,
            environment.srcClientId,
            D3_CLIENT_ENABLE_NOTIFICATIONS,
            "true"
        ).map {
            val withdrawalAccount = integrationHelper.accountHelper.ethWithdrawalAccount
            val tx = Transaction.builder(withdrawalAccount.accountId)
                .transferAsset(
                    withdrawalAccount.accountId,
                    environment.srcClientId,
                    ETH_ASSET_ID,
                    ROLLBACK_DESCRIPTION,
                    rollbackValue.toPlainString()
                )
                .transferAsset(
                    withdrawalAccount.accountId,
                    environment.srcClientId,
                    ETH_ASSET_ID,
                    FEE_ROLLBACK_DESCRIPTION,
                    rollbackFeeValue.toPlainString()
                )
                .setCreatedTime(System.currentTimeMillis())
                .setQuorum(1)
                .sign(withdrawalAccount.keyPair)
                .build()
            integrationHelper.irohaConsumer.send(tx).get()
        }.map {
            Thread.sleep(WAIT_TIME)
            val receivedEmails = getAllMails()
            assertEquals(1, receivedEmails.size)
            val lastEmail = receivedEmails.last()
            assertEquals(D3_ROLLBACK_SUBJECT, lastEmail.subject)
            assertEquals(SRC_USER_EMAIL, lastEmail.to)
            assertEquals(NOTIFICATION_EMAIL, lastEmail.from)
            assertTrue(lastEmail.message.contains("Fee $rollbackFeeValue $ETH_ASSET_ID is rolled back as well."))
            verify(environment.pushService).send(any())
            Unit
        }.failure { ex -> fail(ex) }
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given D3 client with enabled email notifications and notary account
     * @when account receives Ethereum withdrawal proof
     * @then D3 client is notified about withdrawal proof collection
     */
    @Test
    fun testNotificationWithdrawalProof() {
        val withdrawalIrohaTxHash = "5DC4E41457FF8CA7CE7316B3CC471F9C7A641A237389783293A29C71A066E8B8"
        // It's a valid proof given me by Alexey
        val ethWithdrawalProof = EthWithdrawalProof(
            tokenContractAddress = "0xef4777221a5d20cc4e6e6afed03af47bba90ff3c",
            amount = "2340000000000000000",
            beneficiary = "0x82e0b6cc1ea0d0b91f5fc86328b8e613bdaf72e8",
            accountId = environment.srcClientId,
            relay = "0x82e0b6cc1ea0d0b91f5fc86328b8e613bdaf72e8",
            signature = VRSSignature(
                v = BigInteger.valueOf(28),
                s = "6950e38b196a50da615fc0b15557a2a29e8bdd6ecef4751b841b2de9fbdf9ffd",
                r = "b76621ce5b62ce392d883ca67f3888b1d5c5812326e8b882b22f250bfd25975c"
            ),
            irohaHash = withdrawalIrohaTxHash
        )
        integrationHelper.setAccountDetailWithRespectToBrvs(
            environment.srcClientConsumer,
            environment.srcClientId,
            D3_CLIENT_ENABLE_NOTIFICATIONS,
            "true"
        ).map {
            val tx = Transaction.builder(environment.ethWithdrawalProofSetterConsumer.creator)
                .setAccountDetail(
                    integrationHelper.accountHelper.ethProofStorageAccount.accountId,
                    "0xedef306ba72b6245d4d54808eeb017b8eb01fe08",
                    gson.toJson(ethWithdrawalProof).irohaEscape()
                ).build()
            environment.ethWithdrawalProofSetterConsumer.send(tx).get()
        }.map {
            Thread.sleep(WAIT_TIME)
            val soraEvent = getLastSoraEvent(SoraURI.WITHDRAWAL_PROOFS) as SoraEthWithdrawalProofsEvent
            assertEquals(ethWithdrawalProof.accountId, soraEvent.accountIdToNotify)
            assertEquals(ethWithdrawalProof.irohaHash, soraEvent.irohaTxHash)
            assertEquals(ethWithdrawalProof.relay, soraEvent.relay)
            assertEquals(1, soraEvent.proofs.size)
            assertEquals(BigDecimal(ethWithdrawalProof.amount), soraEvent.amount)
            assertEquals(ethWithdrawalProof.beneficiary, soraEvent.to)
            val proof = soraEvent.proofs.first()
            assertEquals(ethWithdrawalProof.signature.r, proof.rHex)
            assertEquals(ethWithdrawalProof.signature.s, proof.sHex)
            assertEquals(ethWithdrawalProof.signature.v, proof.v)
            Unit
        }.failure { ex -> fail(ex) }
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given 2 D3 clients with enabled email notifications
     * @when 1st client sends money to 2nd with no fee
     * @then Both clients are notified
     */
    @Test
    fun testNotificationSimpleTransferNoFee() {
        val transferValue = BigDecimal(1)
        integrationHelper.setAccountDetailWithRespectToBrvs(
            environment.srcClientConsumer,
            environment.srcClientId,
            D3_CLIENT_ENABLE_NOTIFICATIONS,
            "true"
        ).flatMap {
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
                ETH_ASSET_ID,
                "no description",
                transferValue.toPlainString()
            )
        }.map {
            Thread.sleep(WAIT_TIME)
            val mails = getAllMails()
            assertEquals(2, mails.size)
            assertTrue(mails.any { message -> message.to == SRC_USER_EMAIL })
            assertTrue(mails.any { message -> message.to == DEST_USER_EMAIL })
            mails.forEach { message ->
                assertEquals(D3_DEPOSIT_TRANSFER_SUBJECT, message.subject)
                assertEquals(NOTIFICATION_EMAIL, message.from)
                assertFalse(message.message.contains("Fee is"))
                if (message.to == SRC_USER_EMAIL) {
                    assertTrue(message.message.contains("to ${environment.destClientId}"))
                } else if (message.to == DEST_USER_EMAIL) {
                    assertTrue(message.message.contains("from ${environment.srcClientId}"))
                }
            }
            verify(environment.pushService, times(2)).send(any())
            Unit
        }.failure { ex -> fail(ex) }
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given D3 client with disabled notifications and notary account
     * @when notary sends money to D3 client
     * @then D3 client is not notified
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
                ETH_ASSET_ID,
                "no description",
                depositValue.toPlainString(),
                quorum = 1
            )
        }.map {
            Thread.sleep(WAIT_TIME)
            assertTrue(environment.dumbster.receivedEmails.isEmpty())
            verify(environment.pushService, never()).send(any())
            Unit
        }.failure { ex -> fail(ex) }
    }

    /**
     * Returns all the mails from the dumbster SMTP server using HTTP endpoint
     * @return all mails
     */
    private fun getAllMails(): List<DumbsterMessage> {
        val res =
            khttp.get("http://127.0.0.1:${environment.notificationsConfig.debugWebPort}/dumbster/mail/all")
        if (res.statusCode != 200) {
            throw Exception("Cannot get emails. HTTP status code ${res.statusCode}")
        }
        val listType = object : TypeToken<List<DumbsterMessage>>() {}.type
        return gson.fromJson(res.text, listType)
    }

    /**
     * Returns the last posted Sora event
     * @param uri - uri of event(deposit, withdrawal, etc)
     * @return the last posted Sora event
     */
    private fun getLastSoraEvent(uri: SoraURI): SoraEvent {
        val res = khttp.get("http://127.0.0.1:${environment.notificationsConfig.debugWebPort}/sora/all/${uri.uri}")
        if (res.statusCode != 200) {
            throw Exception("Cannot get Sora events. HTTP status code ${res.statusCode}")
        }
        val type: Type = when (uri) {
            SoraURI.DEPOSIT_URI -> SoraDepositEvent::class.java
            SoraURI.WITHDRAWAL_URI -> SoraWithdrawalEvent::class.java
            SoraURI.REGISTRATION_URI -> SoraRegistrationEvent::class.java
            SoraURI.FAILED_REGISTRATION_URI -> SoraFailedRegistrationEvent::class.java
            SoraURI.WITHDRAWAL_PROOFS -> SoraEthWithdrawalProofsEvent::class.java
            else -> throw IllegalArgumentException("URI ${uri.uri} is not supported")
        }
        val jsonArray = JSONArray(res.text)
        val jsonObject: JSONObject = jsonArray.getJSONObject(jsonArray.length() - 1)
        return gson.fromJson(jsonObject.toString(), type)
    }
}
