package registration

import com.github.kittinunf.result.Result
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class RegistrationTest {

    /** Port of testing service  */
    private val port = 8083

    /** Correct user name */
    private val correctName = "green"

    /** Correct whitelist*/
    private val correctWhitelist = listOf("0x3b5db77bf3ea070f561c6a88b230816552bc00c9")

    /** Correct user public key */
    private val correctPubkey = "0f0ce16d2afbb8eca23c7d8c2724f0c257a800ee2bbd54688cec6b898e3f7e33"

    /** Expected ethereum wallet */
    private val correctEthWallet = "newEthWallet"

    /** Registration strategy that always returns true */
    private val strategy: RegistrationStrategy = mock {
        on {
            register(
                com.nhaarman.mockito_kotlin.any(),
                com.nhaarman.mockito_kotlin.any(),
                com.nhaarman.mockito_kotlin.any(),
                com.nhaarman.mockito_kotlin.any()
            )
        } doReturn Result.of { correctEthWallet }
    }

    private val registrationService: Job

    init {
        registrationService = GlobalScope.launch {
            RegistrationServiceEndpoint(port, strategy)
        }

        runBlocking { delay(10_000) }
    }

    @AfterAll
    fun dropDown() {
        registrationService.cancel()
    }

    /**
     * Send POST request to local server
     */
    fun post(params: Map<String, String>): khttp.responses.Response {
        return khttp.post("http://127.0.0.1:$port/users", data = params)
    }

    /**
     * @given Registration service is up.
     * @when Send POST with wrong namegr
     * @then Error response is returned
     */
    @Test
    fun postWrongName() {
        val actual = post(mapOf("wrong_name" to correctName, "pubkey" to correctPubkey))

        assertEquals(HttpStatusCode.BadRequest.value, actual.statusCode)
        assertEquals("Response has been failed. Parameter \"name\" is not specified. ", actual.text)
    }

    /**
     * @given Registration service is up.
     * @when Send POST with wrong pubkey
     * @then Error response is returned
     */
    @Test
    fun postWrongPubkey() {
        val actual = post(mapOf("name" to correctName, "wrong_pubkey" to correctPubkey))

        assertEquals(HttpStatusCode.BadRequest.value, actual.statusCode)
        assertEquals("Response has been failed. Parameter \"pubkey\" is not specified.", actual.text)
    }

    /**
     * @given Registration service is up.
     * @when Send POST with correct name and pubkey
     * @then Success response is returned
     */
    @Test
    fun postCorrect() {
        val actual = post(mapOf("name" to correctName, "pubkey" to correctPubkey))

        assertEquals(HttpStatusCode.OK.value, actual.statusCode)
        assertEquals(correctEthWallet, actual.text)
    }
}
