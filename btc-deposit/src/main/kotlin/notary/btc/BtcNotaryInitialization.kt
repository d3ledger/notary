package notary.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import healthcheck.HealthyService
import helper.network.addPeerConnectionStatusListener
import helper.network.getPeerGroup
import helper.network.startChainDownload
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import model.IrohaCredential
import mu.KLogging
import notary.btc.config.BtcNotaryConfig
import notary.btc.listener.BitcoinBlockChainListener
import org.bitcoinj.core.Address
import org.bitcoinj.core.PeerGroup
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
        val peerGroup =
            getPeerGroup(wallet, btcNetworkConfigProvider.getConfig(), btcNotaryConfig.bitcoin.blockStoragePath)
        addPeerConnectionStatusListener(peerGroup, ::notHealthy, ::cured)
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
            startChainDownload(peerGroup, btcNotaryConfig.bitcoin.host)
        }
    }

    //Checks if address is watched by notary
    fun isWatchedAddress(btcAddress: String) =
        wallet.isAddressWatched(Address.fromBase58(btcNetworkConfigProvider.getConfig(), btcAddress))

    /**
     * Listens to newly registered bitcoin addresses and adds addresses to current wallet object
     */
    private fun listenToRegisteredClients(wallet: Wallet, irohaObservable: Observable<BlockOuterClass.Block>) {
        irohaObservable.subscribeOn(Schedulers.from(Executors.newSingleThreadExecutor()))
            .subscribe({ block ->
                getSetDetailCommands(block).forEach { command ->
                    if (isNewClientWasRegistered(command)) {
                        val address = Address.fromBase58(
                            btcNetworkConfigProvider.getConfig(),
                            command.setAccountDetail.value
                        )
                        //Add new registered address to wallet
                        if (wallet.addWatchedAddress(address)) {
                            logger.info { "New BTC address ${command.setAccountDetail.value} was added to wallet" }
                        } else {
                            logger.error { "Address $address was not added to wallet" }
                        }
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
     * Logger
     */
    companion object : KLogging()
}
