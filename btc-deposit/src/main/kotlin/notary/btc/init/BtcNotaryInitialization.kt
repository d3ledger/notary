package notary.btc.init

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import healthcheck.HealthyService
import helper.network.addPeerConnectionStatusListener
import helper.network.startChainDownload
import io.reactivex.Observable
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import listener.btc.NewBtcClientRegistrationListener
import model.IrohaCredential
import mu.KLogging
import notary.btc.config.BtcNotaryConfig
import notary.btc.factory.createBtcNotary
import notary.btc.listener.BitcoinBlockChainDepositListener
import org.bitcoinj.core.Address
import org.bitcoinj.core.PeerGroup
import org.bitcoinj.utils.BriefLogFormatter
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import provider.NotaryPeerListProviderImpl
import provider.btc.address.BtcRegisteredAddressesProvider
import provider.btc.network.BtcNetworkConfigProvider
import sidechain.SideChainEvent
import sidechain.iroha.IrohaChainListener
import java.io.Closeable


@Component
class BtcNotaryInitialization(
    @Autowired private val peerGroup: PeerGroup,
    @Autowired private val wallet: Wallet,
    @Autowired private val btcNotaryConfig: BtcNotaryConfig,
    @Qualifier("notaryCredential")
    @Autowired private val irohaCredential: IrohaCredential,
    @Autowired private val irohaAPI: IrohaAPI,
    @Autowired private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider,
    @Qualifier("depositIrohaChainListener")
    @Autowired private val irohaChainListener: IrohaChainListener,
    @Autowired private val newBtcClientRegistrationListener: NewBtcClientRegistrationListener,
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider
) : HealthyService(), Closeable {

    /**
     * Init notary
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Btc notary initialization" }
        //Enables short log format for Bitcoin events
        BriefLogFormatter.init()
        addPeerConnectionStatusListener(peerGroup, ::notHealthy, ::cured)
        return irohaChainListener.getIrohaBlockObservable().map { irohaObservable ->
            newBtcClientRegistrationListener.listenToRegisteredClients(
                wallet, irohaObservable
            ) {
                // Kill deposit service if Iroha chain listener is not functioning
                close()
            }
            logger.info { "Registration service listener was successfully initialized" }
        }.flatMap {
            btcRegisteredAddressesProvider.getRegisteredAddresses()
        }.map { registeredAddresses ->
            // Adding previously registered addresses to the wallet
            registeredAddresses.map { btcAddress ->
                Address.fromBase58(
                    btcNetworkConfigProvider.getConfig(),
                    btcAddress.address
                )
            }.forEach { address ->
                wallet.addWatchedAddress(address)
            }
            logger.info { "Previously registered addresses were added to the wallet" }
        }.map {
            getBtcEvents(peerGroup, btcNotaryConfig.bitcoin.confidenceLevel)
        }.map { btcEvents ->
            val queryAPI = QueryAPI(irohaAPI, irohaCredential.accountId, irohaCredential.keyPair)

            val peerListProvider = NotaryPeerListProviderImpl(
                queryAPI,
                btcNotaryConfig.notaryListStorageAccount,
                btcNotaryConfig.notaryListSetterAccount
            )
            val notary = createBtcNotary(irohaCredential, irohaAPI, btcEvents, peerListProvider)
            notary.initIrohaConsumer().failure { ex -> throw ex }
        }.map {
            startChainDownload(peerGroup)
        }
    }

    //Checks if address is watched by notary
    fun isWatchedAddress(btcAddress: String) =
        wallet.isAddressWatched(Address.fromBase58(btcNetworkConfigProvider.getConfig(), btcAddress))

    /**
     * Returns observable object full of deposit events
     */
    private fun getBtcEvents(
        peerGroup: PeerGroup,
        confidenceLevel: Int
    ): Observable<SideChainEvent.PrimaryBlockChainEvent> {
        return Observable.create<SideChainEvent.PrimaryBlockChainEvent> { emitter ->
            peerGroup.addBlocksDownloadedEventListener(
                BitcoinBlockChainDepositListener(
                    btcRegisteredAddressesProvider,
                    emitter,
                    confidenceLevel
                )
            )
        }
    }

    override fun close() {
        logger.info { "Closing Bitcoin notary service" }
        peerGroup.stop()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
