package provider.btc.network

import org.bitcoinj.params.MainNetParams
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("mainnet")
@Component
class BtcMainNetConfigProvider : BtcNetworkConfigProvider {
    override fun getConfig() = MainNetParams.get()
}
