package vendor

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

class EthUsageTest {

    @Test
    fun ethUsage() {
        try {
            val web3 = Web3j.build(HttpService())
            web3.web3ClientVersion().observable().subscribe { x ->
                val clientVersion = x.getWeb3ClientVersion()
                print(clientVersion)
            }
        } catch (e: rx.exceptions.OnErrorNotImplementedException) {
            // if Ethereum client is not running
            assert(true)
        }
    }
}
