package notary.endpoint

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.squareup.moshi.Moshi
import io.ktor.http.HttpStatusCode
import notary.endpoint.eth.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger

/**
 * Fixture for testing notary.endpoint
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RefundServerEndpointTest {

    /** Server initialization bundle */
    val serverEthBundle = ServerInitializationBundle(8080, "eth")
    val serverIrohaBundle = ServerInitializationBundle(8080, "ethwdrb")

    /** JSON adapter */
    val moshi =
        Moshi.Builder().add(EthNotaryResponseMoshiAdapter()).add(BigInteger::class.java, BigIntegerMoshiAdapter())
            .build()

    /** Stub for Ethereum refund request, should represent tx hash from Iroha */
    private val ethRequest = EthRefundRequest("tx_hash_from_iroha")

    /** Successful response */
    private val successResponse = EthNotaryResponse.Successful(
        "signature",
        EthRefund("address", "coin", "10", "irohaTxHash", "")
    )

    /** Strategy mock that always returns success */
    private val ethRefundStrategyMock = mock<EthRefundStrategy> {
        val request = any<EthRefundRequest>()
        on {
            performRefund(request)
        } doReturn successResponse
    }

    private val irohaRefundStrategyMock = mock<IrohaRefundStrategy> {
        val request = any<IrohaRefundRequest>()
        on {
            performRefund(request)
        } doReturn IrohaNotaryResponse.Successful("someHashHere")
    }

    val server = RefundServerEndpoint(serverEthBundle, serverIrohaBundle, ethRefundStrategyMock, irohaRefundStrategyMock)

    /**
     * @given initialized server class
     * @when  call onCallEthRefund()
     * @then  check that answer returns success
     */
    @Test
    fun onEthRefundCallTest() {
        val request = moshi.adapter(EthRefundRequest::class.java).toJson(ethRequest)
        val result = server.onCallEthRefund(request)

        assertEquals(HttpStatusCode.OK, result.code)
        assertEquals(successResponse, moshi.adapter(EthNotaryResponse::class.java).fromJson(result.message))
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

        assertEquals(HttpStatusCode.BadRequest, refundAnswer.code)
        assertEquals(failureResponse.reason, refundAnswer.message)
    }
}
