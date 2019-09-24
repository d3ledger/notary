/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.registration.NotaryRegistrationConfig
import com.d3.commons.registration.main
import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotaryRegistrationTest {

    val registrationConfig =
        loadRawLocalConfigs(
            "registration",
            NotaryRegistrationConfig::class.java,
            "registration.properties"
        )

    init {
        GlobalScope.launch {
            main()
        }

        runBlocking { delay(20_000) }
    }

    /**
     * Send POST request to local server
     */
    fun post(params: Map<String, String>): khttp.responses.Response {
        return khttp.post("http://127.0.0.1:${registrationConfig.port}/users", data = params)
    }

    /**
     * Test health check
     * @given Registration service is up and running
     * @when GET query sent to health check port
     * @then {"status":"UP"} is returned
     */
    @Test
    fun healthcheck() {
        val res = khttp.get("http://127.0.0.1:${registrationConfig.port}/actuator/health")
        assertEquals(200, res.statusCode)
        assertEquals("{\"status\":\"UP\"}", res.text)
    }

    /**
     * Test registration
     * @given Registration service is up and running
     * @when POST query is sent to register a user with `name` and `pubkey`
     * @then new account is created in Iroha
     */
    @Test
    fun correctRegistration() {
        val name = String.getRandomString(7)
        val pubkey = Ed25519Sha3().generateKeypair().public.toHexString()

        val res = post(
            mapOf(
                "name" to name,
                "pubkey" to pubkey
            )
        )

        assertEquals(200, res.statusCode)
    }

    /**
     * Test registration
     * @given Registration service is up and running
     * @when POST query is sent to register a user with invalid pubkey
     * @then error response is returned
     */
    @Test
    fun invalidClientNameRegistration() {
        val name = "wrong name !%"
        val pubkey = Ed25519Sha3().generateKeypair().public.toHexString()

        val res = post(
            mapOf(
                "name" to name,
                "pubkey" to pubkey
            )
        )

        assertEquals(500, res.statusCode)
    }

    /**
     * Test registration
     * @given Registration service is up and running
     * @when POST query is sent to register a user with invalid pubkey
     * @then error response is returned
     */
    @Test
    fun invalidPubkeyRegistration() {
        val name = String.getRandomString(7)
        val pubkey = "wrong key %!"

        val res = post(
            mapOf(
                "name" to name,
                "pubkey" to pubkey
            )
        )

        assertEquals(500, res.statusCode)
    }

    /**
     * Test registration
     * @given Registration service is up and running
     * @when POST query is sent to register a user with invalid length pubkey
     * @then error response is returned
     */
    @Test
    fun invalidLenthPubkeyRegistration() {
        val name = String.getRandomString(7)
        val pubkey = "123456"

        val res = post(
            mapOf(
                "name" to name,
                "pubkey" to pubkey
            )
        )

        assertEquals(500, res.statusCode)
    }

    /**
     * Registration with same user id and key
     * @given client is registered with clientId and publicKey
     * @when registration request with the same clientId and publicKey is sent
     * @then OK status is returned
     */
    @Test
    fun doubleRegistrationWithSamePubkey() {
        val name = String.getRandomString(7)
        val pubkey = Ed25519Sha3().generateKeypair().public.toHexString()

        // 1st request - registrtation
        val res1 = post(
            mapOf(
                "name" to name,
                "pubkey" to pubkey
            )
        )
        assertEquals(200, res1.statusCode)

        // 2nd request - already registered - OK response
        val res2 = post(
            mapOf(
                "name" to name,
                "pubkey" to pubkey
            )
        )
        assertEquals(200, res2.statusCode)
    }

    /**
     * Register with same user id and different public key
     * @given client is registered with clientId and publicKey
     * @when registration request with the same clientId and different publicKey is sent
     * @then Error status is returned
     */
    @Test
    fun doubleRegistrationWithDifferentPubkey() {
        val name = String.getRandomString(7)
        val pubkey = Ed25519Sha3().generateKeypair().public.toHexString()

        // 1st request - registrtation
        val res1 = post(
            mapOf(
                "name" to name,
                "pubkey" to pubkey
            )
        )
        assertEquals(200, res1.statusCode)

        val otherPubkey = Ed25519Sha3().generateKeypair().public.toHexString()
        // 2nd request - already registered - OK response
        val res2 = post(
            mapOf(
                "name" to name,
                "pubkey" to otherPubkey
            )
        )
        assertEquals(500, res2.statusCode)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
