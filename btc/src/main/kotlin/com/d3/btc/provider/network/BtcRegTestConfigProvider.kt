package com.d3.btc.provider.network

import org.bitcoinj.params.RegTestParams
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local")
@Component
class BtcRegTestConfigProvider : BtcNetworkConfigProvider {
    override fun getConfig() = RegTestParams.get()
}
