package notary.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import io.reactivex.Observable
import mu.KLogging
import notary.NotaryConfig
import notary.createBtcNotary
import notary.eth.EthNotaryInitialization
import org.bitcoinj.core.BlockChain
import org.bitcoinj.core.Context
import org.bitcoinj.core.PeerGroup
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.store.LevelDBBlockStore
import org.bitcoinj.wallet.Wallet
import sidechain.SideChainEvent
import java.io.File
import java.math.BigInteger

class BtcNotaryInitialization(
    private val notaryConfig: NotaryConfig
) {
    /**
     * Init notary
     */
    fun init(): Result<Unit, Exception> {
        EthNotaryInitialization.logger.info { "Btc notary initialization" }
        return Result.of<Wallet> {
            val walletFile = File(notaryConfig.bitcoin.walletPath)
            Wallet.loadFromFile(walletFile)
        }.map { wallet ->
            getBtcEvents(wallet)
        }.map { btcEvents ->
            val notary = createBtcNotary(notaryConfig, btcEvents)
            notary.initIrohaConsumer()
            Unit
        }
    }

    /**
     * Returns observable object full of given wallet deposit events
     */
    private fun getBtcEvents(wallet: Wallet): Observable<SideChainEvent.PrimaryBlockChainEvent> {
        logger.info { "current wallet $wallet" }
        //TODO Test mode is on. Move to real network or make it configurable
        val networkParams = RegTestParams.get()
        val levelDbFolder = File(notaryConfig.bitcoin.blockStoragePath)
        val blockStore = LevelDBBlockStore(Context(networkParams), levelDbFolder);
        val blockChain = BlockChain(networkParams, wallet, blockStore)
        val peerGroup = PeerGroup(networkParams, blockChain)
        peerGroup.startAsync()
        peerGroup.downloadBlockChain()
        return Observable.create<SideChainEvent.PrimaryBlockChainEvent> { emitter ->
            wallet.addCoinsReceivedEventListener { curWallet, tx, prevBalance, newBalance ->
                val event = SideChainEvent.PrimaryBlockChainEvent.OnPrimaryChainDeposit(
                    tx.hashAsString,
                    //TODO change to real account when btc registration process will be completed
                    "test@notary",
                    "btc",
                    BigInteger.valueOf(newBalance.getValue() - prevBalance.getValue()),
                    ""
                )
                emitter.onNext(event)
            }
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
