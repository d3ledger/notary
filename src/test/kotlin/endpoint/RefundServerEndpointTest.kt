package endpoint

import com.nhaarman.mockito_kotlin.mock
import endpoint.eth.EthRefundStrategy
import org.junit.Test

class RefundServerEndpointTest {

    @Test
    fun onCallTest() {
        val server = RefundServerEndpoint(ServerInitializationBundle(8080, "eth"), mock<EthRefundStrategy> {
            on {
                //                performRefund(any()) doReturn mock<EthNotaryResponse>()
            }
        })
    }
}
