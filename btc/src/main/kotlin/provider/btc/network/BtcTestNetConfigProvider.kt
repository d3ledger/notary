package provider.btc.network

import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.TestNet3Params
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("testnet")
@Component
class BtcTestNetConfigProvider : BtcNetworkConfigProvider {
    override fun getConfig(): NetworkParameters {
        return TestNet3Params.get()!!
    }
}
