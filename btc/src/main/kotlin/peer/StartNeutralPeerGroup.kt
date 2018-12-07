package peer

import com.google.common.util.concurrent.ListenableFuture
import helper.network.getBlockChain
import mu.KLogging

import org.bitcoinj.core.PeerGroup
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import provider.btc.network.BtcNetworkConfigProvider

/**
 * This is a peer group implementation that can be used in multiple services simultaneously with no fear of get exception while calling 'startAsync()' twice
 */
@Component
class StartNeutralPeerGroup(
    @Autowired btcNetworkConfigProvider: BtcNetworkConfigProvider,
    @Autowired wallet: Wallet,
    @Qualifier("blockStoragePath")
    @Autowired blockStoragePath: String
) :
    PeerGroup(
        btcNetworkConfigProvider.getConfig(),
        getBlockChain(wallet, btcNetworkConfigProvider.getConfig(), blockStoragePath)
    ) {

    private var started = false

    @Synchronized
    override fun startAsync(): ListenableFuture<*>? {
        if (!started) {
            val asyncStart = super.startAsync()
            started = true
            return asyncStart
        }
        logger.warn { "Cannot start peer group, because it was started previously." }
        return null
    }

    @Synchronized
    override fun downloadBlockChain() {
        if (started) {
            super.downloadBlockChain()
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}