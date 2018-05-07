package vendor

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

class EthUsageTest {

    @Test
    fun ethUsage() {
        Assertions.assertThrows(rx.exceptions.OnErrorNotImplementedException::class.java, {
            val web3 = Web3j.build(HttpService())
            web3.web3ClientVersion().observable().subscribe { x ->
                val clientVersion = x.getWeb3ClientVersion()
                print(clientVersion)
            }
        })
    }
}
