package registration

import com.github.kittinunf.result.Result
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import kotlinx.coroutines.experimental.async
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class RegistrationTest {

    /** Port of testing service  */
    private val port = 8083

    /** Registration strategy that always returns true */
    private val strategy: RegistrationStrategy = mock {
        on {
            register(
                com.nhaarman.mockito_kotlin.any(),
                com.nhaarman.mockito_kotlin.any()
            )
        } doReturn Result.of { }
    }

    @BeforeAll
    fun init() {
        async {
            RegistrationServiceEndpoint(port, strategy)
        }

        Thread.sleep(3_000)
    }

    /**
     * Send POST request to lokal server
     */
    fun post(params: Map<String, String>): String {
        val res = khttp.post("http://127.0.0.1:$port/register", data = params)

        return res.text
    }

    /**
     * @given Registration service is up.
     * @when Send POST with wrong name
     * @then Error response is returned
     */
    @Test
    fun postWrongName() {
        val actual = post(mapOf("wrong_name" to "MrGreen", "pubkey" to "MyLittleSecret"))
        assertEquals("Response has been failed. Parameter \"name\" is not specified.", actual)
    }

    /**
     * @given Registration service is up.
     * @when Send POST with wrong pubkey
     * @then Error response is returned
     */
    @Test
    fun postWrongPubkey() {
        val actual = post(mapOf("name" to "MrGreen", "wrong_pubkey" to "MyLittleSecret"))
        assertEquals("Response has been failed. Parameter \"pubkey\" is not specified.", actual)
    }

    /**
     * @given Registration service is up.
     * @when Send POST with correct name and pubkey
     * @then Success response is returned
     */
    @Test
    fun postCorrect() {
        val actual = post(mapOf("name" to "MrGreen", "pubkey" to "MyLittleSecret"))
        assertEquals("OK", actual)
    }
}
