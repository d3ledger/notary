package provider.btc.network

import org.bitcoinj.core.NetworkParameters

interface BtcNetworkConfigProvider {
    fun getConfig(): NetworkParameters
}
