package com.d3.btc.provider.network

import org.bitcoinj.core.NetworkParameters

interface BtcNetworkConfigProvider {
    fun getConfig(): NetworkParameters
}
