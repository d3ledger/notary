/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package notifications

import com.d3.commons.registration.FAILED_REGISTRATION_KEY
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.util.GsonInstance
import com.d3.commons.util.irohaEscape
import com.d3.commons.util.toHexString
import com.d3.notifications.event.*
import com.d3.notifications.init.ETH_WALLET
import com.d3.notifications.provider.EthWithdrawalProof
import com.d3.notifications.provider.VRSSignature
import com.d3.notifications.service.ETH_ASSET_ID
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import integration.helper.IrohaIntegrationHelperUtil
import jp.co.soramitsu.iroha.java.Transaction
import notifications.environment.NotificationsIntegrationTestEnvironment
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import java.math.BigDecimal
import java.math.BigInteger

private const val WAIT_TIME = 5_000L

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

    @AfterAll
    fun closeEnvironments() {
        environment.close()
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given D3 client  and notary account
     * @when notary sends money to D3 client
     * @then D3 client is notified about deposit
     */
    @Test
    fun testNotificationDeposit() {
        val depositValue = BigDecimal(1)
        val from = "0x123"
        Result.of {
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
            val soraEvent = gson.fromJson(environment.getLastSoraEvent().toString(), SoraDepositEvent::class.java)
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
     * @given D3 client and Ethereum registration service
     * @when Ethereum registration service registers D3 client in the Ethereum subsystem
     * @then D3 client is notified about registration
     */
    @Test
    fun testNotificationEthRegistration() {
        val ethAddress = "abc"
        Result.of {
            val ethRegistrationAccount = integrationHelper.accountHelper.ethRegistrationAccount
            integrationHelper.setAccountDetail(
                IrohaConsumerImpl(ethRegistrationAccount, integrationHelper.irohaAPI),
                environment.srcClientConsumer.creator,
                ETH_WALLET,
                ethAddress
            )
        }.map {
            Thread.sleep(WAIT_TIME)
            val soraEvent = gson.fromJson(environment.getLastSoraEvent().toString(), SoraRegistrationEvent::class.java)
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
     * @given D3 client and Ethereum registration service
     * @when Ethereum registration service fails to register D3 client in the Ethereum subsystem
     * @then D3 client is notified about failed registration
     */
    @Test
    fun testNotificationFailedEthRegistration() {
        Result.of {
            val ethRegistrationAccount = integrationHelper.accountHelper.ethRegistrationAccount
            integrationHelper.setAccountDetail(
                IrohaConsumerImpl(ethRegistrationAccount, integrationHelper.irohaAPI),
                environment.srcClientConsumer.creator,
                FAILED_REGISTRATION_KEY, ""
            )
        }.map {
            Thread.sleep(WAIT_TIME)
            val soraEvent =
                gson.fromJson(environment.getLastSoraEvent().toString(), SoraFailedRegistrationEvent::class.java)
            assertEquals(RegistrationEventSubsystem.ETH.name, soraEvent.subsystem)
            assertEquals(environment.srcClientConsumer.creator, soraEvent.accountIdToNotify)
            assertNotNull(soraEvent.id)
            assertNotNull(soraEvent.time)
            Unit
        }.failure { ex -> fail(ex) }
    }

    /**
     * Note: Iroha must be deployed to pass the test.
     * @given D3 client and notary account
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
        Result.of {
            val tx = Transaction.builder(environment.ethWithdrawalProofSetterConsumer.creator)
                .setAccountDetail(
                    integrationHelper.accountHelper.ethProofStorageAccount.accountId,
                    "0xedef306ba72b6245d4d54808eeb017b8eb01fe08",
                    gson.toJson(ethWithdrawalProof).irohaEscape()
                ).build()
            environment.ethWithdrawalProofSetterConsumer.send(tx).get()
        }.map {
            Thread.sleep(WAIT_TIME)
            val soraEvent =
                gson.fromJson(environment.getLastSoraEvent().toString(), SoraEthWithdrawalProofsEvent::class.java)
            assertEquals(ethWithdrawalProof.accountId, soraEvent.accountIdToNotify)
            assertEquals(ethWithdrawalProof.irohaHash, soraEvent.irohaTxHash)
            assertEquals(ethWithdrawalProof.relay, soraEvent.relay)
            assertEquals(1, soraEvent.proofs.size)
            assertEquals(BigDecimal(ethWithdrawalProof.amount), soraEvent.amount)
            assertEquals(ethWithdrawalProof.beneficiary, soraEvent.to)
            val proof = soraEvent.proofs.first()
            assertEquals(ethWithdrawalProof.signature.r, proof.r)
            assertEquals(ethWithdrawalProof.signature.s, proof.s)
            assertEquals(ethWithdrawalProof.signature.v, proof.v)
            Unit
        }.failure { ex -> fail(ex) }
    }
}
