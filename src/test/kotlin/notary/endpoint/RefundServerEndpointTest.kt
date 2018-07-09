package notary.endpoint

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.squareup.moshi.Moshi
import notary.endpoint.eth.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger

/**
 * Fixture for testing notary.endpoint
 */
class RefundServerEndpointTest {

    val moshi =
        Moshi.Builder().add(EthNotaryResponseMoshiAdapter()).add(BigInteger::class.java, BigIntegerMoshiAdapter())
            .build()

    /**
     * @given initialized server class
     * @when  call ::inCallEthRefund
     * @then  check that answer is expected
     */
    @Test
    fun onEthRefundCallTest() {
        val server = RefundServerEndpoint(
            ServerInitializationBundle(8080, "eth"),
            ethRefundStrategyMock
        )

        val request = moshi.adapter(EthRefundRequest::class.java).toJson(ethRequest)
        val refundAnswer = server.onCallEthRefund(request)

        println("Answer = $refundAnswer")

        assertEquals(response, moshi.adapter(EthNotaryResponse::class.java).fromJson(refundAnswer))
    }

    private val ethRequest = EthRefundRequest("tx_hash_from_iroha")

    private val response = EthNotaryResponse.Successful(
        "signature",
        EthRefund("address", "coin", BigInteger.TEN, "irohaTxHash")
    )

    private val ethRefundStrategyMock = mock<EthRefundStrategy> {
        val request = any<EthRefundRequest>()
        on {
            performRefund(request)
        } doReturn response
    }
}
