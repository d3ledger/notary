package notary.endpoint

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.squareup.moshi.Moshi
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.experimental.async
import notary.endpoint.eth.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import registration.RegistrationServiceEndpoint
import java.math.BigInteger

/**
 * Fixture for testing notary.endpoint
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RefundServerEndpointTest {

    /** Server port */
    val port = 8080

    /** Refund server */
    lateinit var server: RefundServerEndpoint

    /** Server initialization bundle */
    val serverBundle = ServerInitializationBundle(8080, "eth")

    /** JSON adapter */
    val moshi =
        Moshi.Builder().add(EthNotaryResponseMoshiAdapter()).add(BigInteger::class.java, BigIntegerMoshiAdapter())
            .build()

    val successResponse = EthNotaryResponse.Successful(
        "signature",
        EthRefund("address", "coin", "10", "irohaTxHash")
    )

    /** Strategy mock that always returns success */
    private val ethRefundStrategyMock = mock<EthRefundStrategy> {
        val request = any<EthRefundRequest>()
        on {
            performRefund(request)
        } doReturn successResponse
    }

    /**
     * Send GET request to local server
     */
    fun get(hash: String): khttp.responses.Response {
        return khttp.get("http://127.0.0.1:$port/eth/$hash")
    }

    @BeforeAll
    fun init() {
        async {
            server = RefundServerEndpoint(serverBundle, ethRefundStrategyMock)
        }

        Thread.sleep(3_000)
    }

    /**
     * @given initialized server class
     * @when  call onCallEthRefund()
     * @then  check that answer returns success
     */
    @Test
    fun onEthRefundCallTest() {
        val actual = khttp.get("http://127.0.0.1:$port/eth/some_tx_hash")

        assertEquals(HttpStatusCode.OK.value, actual.statusCode)
        assertEquals(successResponse, moshi.adapter(EthNotaryResponse::class.java).fromJson(actual.text))
    }

    /**
     * @given initialized server class
     * @when  call onCallEthRefund() with null parameter
     * @then  check that answer returns key not found
     */
    @Test
    fun emptyCall() {
        val failureResponse = EthNotaryResponse.Error("Request has been failed. Error in URL")

        val refundAnswer = server.onCallEthRefund(null)

        assertEquals(failureResponse.reason, refundAnswer)
    }
}
