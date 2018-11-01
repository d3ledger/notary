package helper.network

import mu.KLogging
import org.bitcoinj.core.BlockChain
import org.bitcoinj.core.Context
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.PeerGroup
import org.bitcoinj.store.LevelDBBlockStore
import org.bitcoinj.wallet.Wallet
import java.io.File
import java.net.InetAddress

private val logger = KLogging().logger

/**
 * Starts bitcoin blockchain downloading process
 */
fun startChainDownload(peerGroup: PeerGroup, host: String) {
    logger.info { "Start bitcoin blockchain download" }
    peerGroup.addAddress(InetAddress.getByName(host))
    peerGroup.startAsync()
    peerGroup.downloadBlockChain()
}

/**
 * Returns group of peers
 */
fun getPeerGroup(wallet: Wallet, networkParameters: NetworkParameters, blockStoragePath: String): PeerGroup {
    val levelDbFolder = File(blockStoragePath)
    val blockStore = LevelDBBlockStore(Context(networkParameters), levelDbFolder)
    val blockChain = BlockChain(networkParameters, wallet, blockStore)
    return PeerGroup(networkParameters, blockChain)
}
