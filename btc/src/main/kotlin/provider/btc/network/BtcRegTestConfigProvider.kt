package provider.btc.network

import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.RegTestParams
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local")
@Component
class BtcRegTestConfigProvider : BtcNetworkConfigProvider {
    override fun getConfig(): NetworkParameters {
        return RegTestParams.get()
    }
}
