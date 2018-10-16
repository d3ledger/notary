package notary.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import io.reactivex.Observable
import model.IrohaCredential
import mu.KLogging
import notary.btc.config.BtcNotaryConfig
import notary.btc.listener.BitcoinBlockChainListener
import org.bitcoinj.core.BlockChain
import org.bitcoinj.core.Context
import org.bitcoinj.core.PeerGroup
import org.bitcoinj.store.LevelDBBlockStore
import org.bitcoinj.utils.BriefLogFormatter
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import provider.NotaryPeerListProviderImpl
import provider.btc.address.BtcRegisteredAddressesProvider
import provider.btc.network.BtcNetworkConfigProvider
import sidechain.SideChainEvent
import sidechain.iroha.consumer.IrohaNetwork
import java.io.File
import java.net.InetAddress

@Component
class BtcNotaryInitialization(
    @Autowired private val btcNotaryConfig: BtcNotaryConfig,
    @Autowired private val irohaCredential: IrohaCredential,
    @Autowired private val irohaNetwork: IrohaNetwork,
    @Autowired private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider,
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider
) {

    /**
     * Init notary
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Btc notary initialization" }
        //Enables short log format for Bitcoin events
        BriefLogFormatter.init()
        val peerGroup = getPeerGroup()
        return Result.of {
            getBtcEvents(peerGroup, btcNotaryConfig.bitcoin.confidenceLevel)
        }.map { btcEvents ->
            val peerListProvider = NotaryPeerListProviderImpl(
                irohaCredential,
                irohaNetwork,
                btcNotaryConfig.notaryListStorageAccount,
                btcNotaryConfig.notaryListSetterAccount
            )
            val notary = createBtcNotary(irohaCredential, irohaNetwork, btcEvents, peerListProvider)
            notary.initIrohaConsumer().failure { ex -> throw ex }
        }.map {
            startChainDownload(peerGroup)
        }
    }

    /**
     * Returns observable object full of deposit events
     */
    private fun getBtcEvents(
        peerGroup: PeerGroup,
        confidenceLevel: Int
    ): Observable<SideChainEvent.PrimaryBlockChainEvent> {
        return Observable.create<SideChainEvent.PrimaryBlockChainEvent> { emitter ->
            peerGroup.addBlocksDownloadedEventListener(
                BitcoinBlockChainListener(
                    btcRegisteredAddressesProvider,
                    emitter,
                    confidenceLevel
                )
            )
        }
    }

    /**
     * Returns group of peers
     */
    private fun getPeerGroup(): PeerGroup {
        val wallet = Wallet.loadFromFile(File(btcNotaryConfig.bitcoin.walletPath))
        logger.info { wallet }
        val networkParams = btcNetworkConfigProvider.getConfig()
        val levelDbFolder = File(btcNotaryConfig.bitcoin.blockStoragePath)
        val blockStore = LevelDBBlockStore(Context(networkParams), levelDbFolder)
        val blockChain = BlockChain(networkParams, wallet, blockStore)
        return PeerGroup(networkParams, blockChain)
    }

    /**
     * Starts bitcoin blockchain downloading process
     */
    private fun startChainDownload(peerGroup: PeerGroup) {
        logger.info { "Start bitcoin blockchain download" }
        peerGroup.addAddress(InetAddress.getByName(btcNotaryConfig.bitcoin.host))
        peerGroup.startAsync()
        peerGroup.downloadBlockChain()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
