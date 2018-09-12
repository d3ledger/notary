package notary.btc

import io.reactivex.ObservableEmitter
import mu.KLogging
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionConfidence
import org.bitcoinj.wallet.Wallet
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener
import provider.btc.BtcAddressesProvider
import sidechain.SideChainEvent
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean

private const val BTC_ASSET_NAME = "btc"

class ReceivedCoinsListener(
    private val btcAddressesProvider: BtcAddressesProvider,
    private val confidenceLevel: Int,
    private val emitter: ObservableEmitter<SideChainEvent.PrimaryBlockChainEvent>
) : WalletCoinsReceivedEventListener {

    override fun onCoinsReceived(wallet: Wallet, tx: Transaction, prevBalance: Coin, newBalance: Coin) {
        tx.confidence.addEventListener(ConfirmedTxListener(confidenceLevel, tx, ::handleTx))
    }

    private fun handleTx(tx: Transaction) {
        btcAddressesProvider.getAddresses().fold({ addresses ->
            tx.outputs.forEach { output ->
                val btcAddress = output.scriptPubKey.getToAddress(output.params).toBase58()
                val irohaAccount = addresses[btcAddress]
                if (irohaAccount != null) {
                    val event = SideChainEvent.PrimaryBlockChainEvent.OnPrimaryChainDeposit(
                        tx.hashAsString,
                        BigInteger.valueOf(tx.lockTime), irohaAccount,
                        BTC_ASSET_NAME,
                        BigInteger.valueOf(output.value.value).toString(),
                        ""
                    )
                    emitter.onNext(event)
                }
            }

        }, { ex ->
            logger.error("cannot get addresses", ex)
        })
    }

    private class ConfirmedTxListener(
        private val confidenceLevel: Int,
        private val tx: Transaction,
        private val txHandler: (Transaction) -> Unit
    ) : TransactionConfidence.Listener {
        private val processed = AtomicBoolean()
        override fun onConfidenceChanged(
            confidence: TransactionConfidence,
            reason: TransactionConfidence.Listener.ChangeReason
        ) {
            /*
            Due to bitoinj library threading issues, we can miss an event of 'depthInBlocks'
            being exactly 'confidenceLevel'. So we check it to be at least 'confidenceLevel'.
            This leads D3 to handle the same transaction many times. This is why we use a special
            flag to check if it has been handled already.
            */
            if (confidence.depthInBlocks >= confidenceLevel
                && processed.compareAndSet(false, true)
            ) {
                confidence.removeEventListener(this)
                txHandler(tx)
            }
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
