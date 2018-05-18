package endpoint

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.squareup.moshi.Moshi
import endpoint.eth.EthNotaryResponse
import endpoint.eth.EthNotaryResponseMoshiAdapter
import endpoint.eth.EthRefundContract
import endpoint.eth.EthRefundStrategy
import org.junit.Assert
import org.junit.Test

class RefundServerEndpointTest {

    val moshi = Moshi.Builder().add(EthNotaryResponseMoshiAdapter()).build()


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

        val request = moshi.adapter(EthRefundContract::class.java).toJson(ethRequest)
        val refundAnswer = server.onCallEthRefund(request)

        println("Answer = $refundAnswer")

        Assert.assertEquals(response, moshi.adapter(EthNotaryResponse::class.java).fromJson(refundAnswer))
    }

    private val ethRequest = EthRefundContract("address", "doge_coin", 100500F)

    private val response = EthNotaryResponse.Successful(
        "signature",
        "pub_key"
    )

    private val ethRefundStrategyMock = mock<EthRefundStrategy> {
        val request = any<EthRefundContract>()
        on {
            performRefund(request)
        } doReturn response
        on {
            validate(any())
        } doReturn true
    }

}
