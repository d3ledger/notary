package notary.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import healthcheck.HealthyService
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import model.IrohaCredential
import mu.KLogging
import notary.btc.config.BtcNotaryConfig
import notary.btc.listener.BitcoinBlockChainListener
import org.bitcoinj.core.Address
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
import sidechain.iroha.CLIENT_DOMAIN
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.getSetDetailCommands
import java.io.File
import java.net.InetAddress
import java.util.concurrent.Executors

@Component
class BtcNotaryInitialization(
    @Autowired private val btcNotaryConfig: BtcNotaryConfig,
    @Autowired private val irohaCredential: IrohaCredential,
    @Autowired private val irohaNetwork: IrohaNetwork,
    @Autowired private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider,
    @Autowired private val irohaChainListener: IrohaChainListener,
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider
) : HealthyService() {

    private val wallet = Wallet.loadFromFile(File(btcNotaryConfig.bitcoin.walletPath))

    /**
     * Init notary
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Btc notary initialization" }
        //Enables short log format for Bitcoin events
        BriefLogFormatter.init()
        logger.info { "Current wallet state $wallet" }
        val peerGroup = getPeerGroup(wallet)
        addPeerHealthCheck(peerGroup)
        return irohaChainListener.getBlockObservable().map { irohaObservable ->
            listenToRegisteredClients(wallet, irohaObservable)
            logger.info { "Registration service listener was successfully initialized" }
        }.map {
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

    //Returns all currently watched addresses
    fun getWatchedAddresses() = wallet.watchedAddresses

    /**
     * Listens to newly registered bitcoin addresses and adds addresses to current wallet object
     */
    private fun listenToRegisteredClients(wallet: Wallet, irohaObservable: Observable<BlockOuterClass.Block>) {
        irohaObservable.subscribeOn(Schedulers.from(Executors.newSingleThreadExecutor()))
            .subscribe({ block ->
                getSetDetailCommands(block).forEach { command ->
                    if (isNewClientWasRegistered(command)) {
                        //Add new registered address to wallet
                        wallet.addWatchedAddress(
                            Address.fromBase58(
                                btcNetworkConfigProvider.getConfig(),
                                command.setAccountDetail.value
                            )
                        )
                        logger.info { "New BTC address ${command.setAccountDetail.value} was added to wallet" }
                    }
                }
            }, { ex ->
                logger.error("Error on subscribe", ex)
            })
    }

    // Checks if new btc client was registered
    private fun isNewClientWasRegistered(command: Commands.Command): Boolean {
        return command.setAccountDetail.accountId.endsWith("@$CLIENT_DOMAIN")
                && command.setAccountDetail.key == "bitcoin"
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
    private fun getPeerGroup(wallet: Wallet): PeerGroup {
        val networkParams = btcNetworkConfigProvider.getConfig()
        val levelDbFolder = File(btcNotaryConfig.bitcoin.blockStoragePath)
        val blockStore = LevelDBBlockStore(Context(networkParams), levelDbFolder)
        val blockChain = BlockChain(networkParams, wallet, blockStore)
        return PeerGroup(networkParams, blockChain)
    }

    /**
     * Adds health checks for a current peer group
     */
    private fun addPeerHealthCheck(peerGroup: PeerGroup) {
        peerGroup.addDisconnectedEventListener { peer, peerCount ->
            //If no peers left
            if (peerCount == 0) {
                logger.warn { "Out of peers" }
                notHealthy()
            }
        }
        // If new peer connected
        peerGroup.addConnectedEventListener { peer, peerCount -> cured() }
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
