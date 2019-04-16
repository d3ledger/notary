package com.d3.btc.peer

import com.d3.btc.helper.network.getBlockChain
import com.d3.btc.provider.network.BtcNetworkConfigProvider
import com.d3.btc.wallet.WalletInitializer
import com.google.common.util.concurrent.ListenableFuture
import mu.KLogging
import org.bitcoinj.core.PeerGroup
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.net.InetAddress

/**
 * This is a peer group implementation that can be used in multiple services simultaneously with no fear of get exception while calling 'startAsync()' or 'stopAsync()' twice
 */
@Component
class SharedPeerGroup(
    @Autowired btcNetworkConfigProvider: BtcNetworkConfigProvider,
    @Autowired private val wallet: Wallet,
    @Qualifier("blockStoragePath")
    @Autowired blockStoragePath: String,
    @Qualifier("btcHosts")
    @Autowired hosts: List<String>,
    @Autowired private val walletInitializer: WalletInitializer
) :
    PeerGroup(
        btcNetworkConfigProvider.getConfig(),
        getBlockChain(wallet, btcNetworkConfigProvider.getConfig(), blockStoragePath)
    ) {

    init {
        hosts.forEach { host ->
            this.addAddress(InetAddress.getByName(host))
            logger.info { "$host was added to peer group" }
        }
    }

    private var started = false
    private var stopped = false

    /**
     * Returns block by hash
     * @param blockHash - hash of block
     * @return block with given hash if exists and null otherwise
     */
    fun getBlock(blockHash: Sha256Hash) = chain?.blockStore?.get(blockHash)

    @Synchronized
    override fun startAsync(): ListenableFuture<*>? {
        if (!started) {
            // Initialize wallet only once
            walletInitializer.initializeWallet(wallet)
            val asyncStart = super.startAsync()
            started = true
            return asyncStart
        }
        logger.warn { "Cannot start peer group, because it was started previously." }
        return null
    }

    @Synchronized
    override fun stopAsync(): ListenableFuture<*>? {
        if (!stopped) {
            // Close block store if possible
            chain?.blockStore?.close()
            val asyncStop = super.stopAsync()
            stopped = true
            return asyncStop
        }
        logger.warn { "Cannot stop peer group, because it was stopped previously" }
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
