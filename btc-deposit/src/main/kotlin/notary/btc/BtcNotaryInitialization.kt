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
import org.bitcoinj.core.PeerGroup
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.store.LevelDBBlockStore
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import provider.NotaryPeerListProviderImpl
import provider.btc.BtcRegisteredAddressesProvider
import sidechain.SideChainEvent
import sidechain.iroha.util.ModelUtil
import java.io.File

@Component
class BtcNotaryInitialization(
    @Autowired private val btcNotaryConfig: BtcNotaryConfig,
    @Autowired private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider
) {


    /**
     * Init notary
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Btc notary initialization" }
        return Result.of<Wallet> {
            val walletFile = File(btcNotaryConfig.bitcoin.walletPath)
            Wallet.loadFromFile(walletFile)
        }.map { wallet ->
            getBtcEvents(wallet, btcNotaryConfig.bitcoin.confidenceLevel)
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
            Unit
        }
    }


    /**
     * Returns observable object full of given wallet deposit events
     */
    private fun getBtcEvents(wallet: Wallet, confidenceLevel: Int): Observable<SideChainEvent.PrimaryBlockChainEvent> {
        logger.info { "Current BTC wallet $wallet" }
        // TODO - D3-320 - dolgopolov.work - Test mode is on. Move to real network or make it configurable
        val networkParams = RegTestParams.get()
        val levelDbFolder = File(btcNotaryConfig.bitcoin.blockStoragePath)
        // TODO - D3-321 - dolgopolov.work - I can see nasty logs here. Try to fix it
        val blockStore = LevelDBBlockStore(Context(networkParams), levelDbFolder);
        val blockChain = BlockChain(networkParams, wallet, blockStore)
        val peerGroup = PeerGroup(networkParams, blockChain)
        peerGroup.startAsync()
        peerGroup.downloadBlockChain()
        return Observable.create<SideChainEvent.PrimaryBlockChainEvent> { emitter ->
            wallet.addCoinsReceivedEventListener(
                ReceivedCoinsListener(
                    btcRegisteredAddressesProvider,
                    confidenceLevel,
                    emitter
                )
            )
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
