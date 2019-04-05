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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExchangerIntegrationTest {

    private val integrationHelper = IrohaIntegrationHelperUtil()

    private val registrationServiceEnvironment = RegistrationServiceTestEnvironment(integrationHelper)

    private val exchangerService = ExchangerServiceTestEnvironment(integrationHelper)

    init {
        exchangerService.init()
        registrationServiceEnvironment.registrationInitialization.init()

        runBlocking { delay(10_000) }
    }

    @AfterAll
    fun closeEnvironments() {
        exchangerService.close()
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
        val userName = String.getRandomString(7)
        val userKeypair = Ed25519Sha3().generateKeypair()
        val userPubkey = userKeypair.public.toHexString()
        val res = registrationServiceEnvironment.register(userName, userPubkey)
        assertEquals(200, res.statusCode)
        val userId = "$userName@$CLIENT_DOMAIN"

        integrationHelper.addIrohaAssetTo(exchangerService.exchangerAccount.accountId, "xor#sora", "10")
        integrationHelper.addIrohaAssetTo(exchangerService.exchangerAccount.accountId, "ether#ethereum", "10")

        integrationHelper.addIrohaAssetTo(userId, "xor#sora", "1")
        integrationHelper.transferAssetIrohaFromClient(
            userId,
            userKeypair,
            userId,
            exchangerService.exchangerAccount.accountId,
            "xor#sora",
            "ether#ethereum",
            "1"
        )

        Thread.sleep(10000)

        val etherBalance = integrationHelper.getIrohaAccountBalance(userId, "ether#ethereum")
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
        val userName = String.getRandomString(7)
        val userKeypair = Ed25519Sha3().generateKeypair()
        val userPubkey = userKeypair.public.toHexString()
        val res = registrationServiceEnvironment.register(userName, userPubkey)
        assertEquals(200, res.statusCode)
        val userId = "$userName@$CLIENT_DOMAIN"

        integrationHelper.addIrohaAssetTo(userId, "xor#sora", "1")
        integrationHelper.transferAssetIrohaFromClient(
            userId,
            userKeypair,
            userId,
            exchangerService.exchangerAccount.accountId,
            "xor#sora",
            "soramichka#sora",
            "1"
        )

        Thread.sleep(10000)

        val xorBalance = integrationHelper.getIrohaAccountBalance(userId, "xor#sora")
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
        val userName = String.getRandomString(7)
        val userKeypair = Ed25519Sha3().generateKeypair()
        val userPubkey = userKeypair.public.toHexString()
        val res = registrationServiceEnvironment.register(userName, userPubkey)
        assertEquals(200, res.statusCode)
        val userId = "$userName@$CLIENT_DOMAIN"

        integrationHelper.addIrohaAssetTo(exchangerService.exchangerAccount.accountId, "xor#sora", "10")
        integrationHelper.addIrohaAssetTo(exchangerService.exchangerAccount.accountId, "ether#ethereum", "10")

        integrationHelper.addIrohaAssetTo(userId, "xor#sora", "100")
        integrationHelper.transferAssetIrohaFromClient(
            userId,
            userKeypair,
            userId,
            exchangerService.exchangerAccount.accountId,
            "xor#sora",
            "ether#ethereum",
            "100"
        )

        Thread.sleep(10000)

        val xorBalance = integrationHelper.getIrohaAccountBalance(userId, "xor#sora")
        assertEquals("100", xorBalance)
        val etherBalance = integrationHelper.getIrohaAccountBalance(userId, "ether#ethereum")
        assertEquals("0", etherBalance)
    }
}