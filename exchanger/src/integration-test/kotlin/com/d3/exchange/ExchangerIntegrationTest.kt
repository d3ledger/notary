/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.exchange

import com.d3.commons.sidechain.iroha.CLIENT_DOMAIN
import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import com.d3.exchange.util.ExchangerServiceTestEnvironment
import integration.helper.IrohaIntegrationHelperUtil
import integration.registration.RegistrationServiceTestEnvironment
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TRANSFER_WAIT_TIME = 10_000L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExchangerIntegrationTest {

    private val integrationHelper = IrohaIntegrationHelperUtil()

    private val registrationServiceEnvironment =
        RegistrationServiceTestEnvironment(integrationHelper)

    private val exchangerServiceEnvironment = ExchangerServiceTestEnvironment(integrationHelper)

    init {
        exchangerServiceEnvironment.init()
        registrationServiceEnvironment.registrationInitialization.init()

        runBlocking { delay(10_000) }
    }

    @AfterAll
    fun closeEnvironments() {
        exchangerServiceEnvironment.close()
        registrationServiceEnvironment.close()
    }

    /**
     * Test of a correct asset exchange
     * @given Registered user in Iroha
     * @when User sends transfer to an exchange service account
     * @then User gets incoming transaction containing converted asset from the service
     */
    @Test
    fun correctExchange() {
        val tokenA = integrationHelper.createAsset().get()
        val tokenB = integrationHelper.createAsset().get()

        val userName = String.getRandomString(7)
        val userKeypair = Ed25519Sha3().generateKeypair()
        val userPubkey = userKeypair.public.toHexString()
        val res = registrationServiceEnvironment.register(userName, userPubkey)
        assertEquals(200, res.statusCode)
        val userId = "$userName@$CLIENT_DOMAIN"

        integrationHelper.addIrohaAssetTo(
            exchangerServiceEnvironment.exchangerAccount.accountId,
            tokenA,
            "10"
        )
        integrationHelper.addIrohaAssetTo(
            exchangerServiceEnvironment.exchangerAccount.accountId,
            tokenB,
            "10"
        )

        integrationHelper.addIrohaAssetTo(userId, tokenA, "1")
        integrationHelper.transferAssetIrohaFromClient(
            userId,
            userKeypair,
            userId,
            exchangerServiceEnvironment.exchangerAccount.accountId,
            tokenA,
            tokenB,
            "1"
        )

        Thread.sleep(TRANSFER_WAIT_TIME)

        val etherBalance = integrationHelper.getIrohaAccountBalance(userId, tokenB)
        assertTrue(BigDecimal(etherBalance) > BigDecimal.ZERO)
    }

    /**
     * Test of a incorrect asset exchange
     * @given Registered user in Iroha
     * @when User sends transfer to an exchange service account specifying unknown target asset
     * @then User gets rollback transaction
     */
    @Test
    fun rollbackUnknownExchange() {
        val tokenA = integrationHelper.createAsset().get()

        val userName = String.getRandomString(7)
        val userKeypair = Ed25519Sha3().generateKeypair()
        val userPubkey = userKeypair.public.toHexString()
        val res = registrationServiceEnvironment.register(userName, userPubkey)
        assertEquals(200, res.statusCode)
        val userId = "$userName@$CLIENT_DOMAIN"

        integrationHelper.addIrohaAssetTo(
            userId,
            tokenA,
            "1"
        )
        integrationHelper.transferAssetIrohaFromClient(
            userId,
            userKeypair,
            userId,
            exchangerServiceEnvironment.exchangerAccount.accountId,
            tokenA,
            "soramichka#sora",
            "1"
        )

        Thread.sleep(TRANSFER_WAIT_TIME)

        val xorBalance = integrationHelper.getIrohaAccountBalance(userId, tokenA)
        assertEquals("1", xorBalance)
    }

    /**
     * Test of a incorrect asset exchange
     * @given Registered user in Iroha
     * @when User sends transfer to an exchange service account specifying much more volume than can be expected
     * @then User gets rollback transaction
     */
    @Test
    fun rollbackTooMuchExchange() {
        val tokenA = integrationHelper.createAsset().get()
        val tokenB = integrationHelper.createAsset().get()

        val userName = String.getRandomString(7)
        val userKeypair = Ed25519Sha3().generateKeypair()
        val userPubkey = userKeypair.public.toHexString()
        val res = registrationServiceEnvironment.register(userName, userPubkey)
        assertEquals(200, res.statusCode)
        val userId = "$userName@$CLIENT_DOMAIN"
        val tooMuchAmount = "1000000"

        integrationHelper.addIrohaAssetTo(
            exchangerServiceEnvironment.exchangerAccount.accountId,
            tokenA,
            "10"
        )
        integrationHelper.addIrohaAssetTo(
            exchangerServiceEnvironment.exchangerAccount.accountId,
            tokenB,
            "10"
        )

        integrationHelper.addIrohaAssetTo(
            userId,
            tokenA,
            tooMuchAmount
        )
        integrationHelper.transferAssetIrohaFromClient(
            userId,
            userKeypair,
            userId,
            exchangerServiceEnvironment.exchangerAccount.accountId,
            tokenA,
            tokenB,
            tooMuchAmount
        )

        Thread.sleep(TRANSFER_WAIT_TIME)

        val xorBalance = integrationHelper.getIrohaAccountBalance(userId, tokenA)
        assertEquals(tooMuchAmount, xorBalance)
        val etherBalance = integrationHelper.getIrohaAccountBalance(userId, tokenB)
        assertEquals("0", etherBalance)
    }

    /**
     * Test of a incorrect asset exchange
     * @given Registered user in Iroha
     * @when User sends transfer to an exchange service account specifying much less volume than can be expected
     * @then User gets rollback transaction
     */
    @Test
    fun rollbackTooLittleExchange() {
        val tokenA = integrationHelper.createAsset().get()
        val tokenB = integrationHelper.createAsset().get()

        val userName = String.getRandomString(7)
        val userKeypair = Ed25519Sha3().generateKeypair()
        val userPubkey = userKeypair.public.toHexString()
        val res = registrationServiceEnvironment.register(userName, userPubkey)
        assertEquals(200, res.statusCode)
        val userId = "$userName@$CLIENT_DOMAIN"

        integrationHelper.addIrohaAssetTo(
            exchangerServiceEnvironment.exchangerAccount.accountId,
            tokenA,
            "10000"
        )
        integrationHelper.addIrohaAssetTo(
            exchangerServiceEnvironment.exchangerAccount.accountId,
            tokenB,
            "0.00000001"
        )

        integrationHelper.addIrohaAssetTo(
            userId,
            tokenA,
            "1"
        )
        integrationHelper.transferAssetIrohaFromClient(
            userId,
            userKeypair,
            userId,
            exchangerServiceEnvironment.exchangerAccount.accountId,
            tokenA,
            tokenB,
            "0.000000000000000001"
        )

        Thread.sleep(TRANSFER_WAIT_TIME)

        val xorBalance = integrationHelper.getIrohaAccountBalance(userId, tokenA)
        assertEquals("1.000000000000000000", xorBalance)
        val etherBalance = integrationHelper.getIrohaAccountBalance(userId, tokenB)
        assertEquals("0", etherBalance)
    }
}
