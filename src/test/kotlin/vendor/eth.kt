package vendor

import org.junit.Test
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService



class EthUsage {
    @Test(expected = rx.exceptions.OnErrorNotImplementedException::class)
    fun ethUsage() {
        val web3 = Web3j.build(HttpService())
        web3.web3ClientVersion().observable().subscribe { x ->
            val clientVersion = x.getWeb3ClientVersion();
            print(clientVersion)
        }
    }
}