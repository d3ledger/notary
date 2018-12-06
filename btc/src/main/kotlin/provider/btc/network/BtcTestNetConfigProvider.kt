package provider.btc.network

import org.bitcoinj.params.TestNet3Params
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("testnet")
@Component
class BtcTestNetConfigProvider : BtcNetworkConfigProvider {
    override fun getConfig() = TestNet3Params.get()
}
