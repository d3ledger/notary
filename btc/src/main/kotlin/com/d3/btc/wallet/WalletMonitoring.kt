package com.d3.btc.wallet

import com.d3.btc.monitoring.Monitoring
import com.d3.btc.provider.network.BtcNetworkConfigProvider
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/*
 * Wallet monitoring service
 */
@Component
class WalletMonitoring(
    @Autowired private val wallet: Wallet,
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider
) : Monitoring() {
    override fun monitor() = WalletSnippet(
        wallet.watchedScripts.map { script -> script.getToAddress(btcNetworkConfigProvider.getConfig()).toString() },
        wallet.unspents.map { unspent -> "$unspent DEPTH ${unspent.parentTransactionDepthInBlocks}" },
        wallet.unspents.sumBy { unspent -> unspent.value.value.toInt() })

    /*
     * Wallet is not suitable for JSON serialization.
     * This is why we have this small data class that holds only critical information about current wallet
     */
    data class WalletSnippet(
        val watchedMultiSigAddresses: List<String>,
        val unspentTransactions: List<String>,
        val totalUnspentValueSat: Int
    )
}
