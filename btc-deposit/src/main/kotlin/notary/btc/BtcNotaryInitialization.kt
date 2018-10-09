package notary.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import io.reactivex.Observable
import model.IrohaCredential
import mu.KLogging
import notary.btc.config.BtcNotaryConfig
import notary.btc.listener.ReceivedCoinsListener
import org.bitcoinj.core.BlockChain
import org.bitcoinj.core.Context
import org.bitcoinj.core.PeerAddress
import org.bitcoinj.core.PeerGroup
import org.bitcoinj.store.LevelDBBlockStore
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import provider.NotaryPeerListProviderImpl
import provider.btc.BtcRegisteredAddressesProvider
import provider.btc.network.BtcNetworkConfigProvider
import sidechain.SideChainEvent
import sidechain.iroha.util.ModelUtil
import wallet.WalletFile
import java.io.File
import java.net.InetAddress

@Component
class BtcNotaryInitialization(
    @Autowired private val btcNotaryConfig: BtcNotaryConfig,
    @Autowired private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider,
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider
) {

    /**
     * Init notary
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Btc notary initialization" }
        val file = File(btcNotaryConfig.bitcoin.walletPath)
        val wallet = Wallet.loadFromFile(file)
        val walletFile = WalletFile(wallet, file)
        return Result.of {
            getBtcEvents(walletFile, btcNotaryConfig.bitcoin.confidenceLevel)
        }.fanout {
            ModelUtil.loadKeypair(
                btcNotaryConfig.notaryCredential.pubkeyPath,
                btcNotaryConfig.notaryCredential.privkeyPath
            )
        }.map { (btcEvents, keypair) ->
            val credential = IrohaCredential(btcNotaryConfig.notaryCredential.accountId, keypair)
            val peerListProvider = NotaryPeerListProviderImpl(
                btcNotaryConfig.iroha,
                credential,
                btcNotaryConfig.notaryListStorageAccount,
                btcNotaryConfig.notaryListSetterAccount
            )
            val notary = createBtcNotary(btcNotaryConfig, credential, btcEvents, peerListProvider)
            notary.initIrohaConsumer().failure { ex -> throw ex }
        }.map {
            startChainDownload(walletFile.wallet)
        }
    }

    /**
     * Returns observable object full of given wallet deposit events
     */
    private fun getBtcEvents(
        walletFile: WalletFile,
        confidenceLevel: Int
    ): Observable<SideChainEvent.PrimaryBlockChainEvent> {
        logger.info { "Current BTC wallet ${walletFile.wallet}" }
        return Observable.create<SideChainEvent.PrimaryBlockChainEvent> { emitter ->
            walletFile.wallet.addCoinsReceivedEventListener(
                ReceivedCoinsListener(
                    btcRegisteredAddressesProvider,
                    confidenceLevel,
                    emitter,
                    walletFile
                )
            )
        }
    }

    /**
     * Starts bitcoin blockchain downloading process
     */
    private fun startChainDownload(wallet: Wallet) {
        logger.info { "Start bitcoin blockchain download" }
        val networkParams = btcNetworkConfigProvider.getConfig()
        val levelDbFolder = File(btcNotaryConfig.bitcoin.blockStoragePath)
        val blockStore = LevelDBBlockStore(Context(networkParams), levelDbFolder);
        val blockChain = BlockChain(networkParams, wallet, blockStore)
        val peerGroup = PeerGroup(networkParams, blockChain)
        peerGroup.addAddress(InetAddress.getByName(btcNotaryConfig.bitcoin.host))
        peerGroup.startAsync()
        peerGroup.downloadBlockChain()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
