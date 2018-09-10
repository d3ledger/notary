package notary.btc

import io.reactivex.ObservableEmitter
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Transaction
import org.bitcoinj.wallet.Wallet
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener
import provider.btc.BtcAddressesProvider
import sidechain.SideChainEvent
import java.math.BigInteger

private const val BTC_ASSET_NAME = "btc"

class ReceivedCoinsListener(
    private val btcAddressesProvider: BtcAddressesProvider,
    private val emitter: ObservableEmitter<SideChainEvent.PrimaryBlockChainEvent>
) : WalletCoinsReceivedEventListener {

    override fun onCoinsReceived(wallet: Wallet, tx: Transaction, prevBalance: Coin, newBalance: Coin) {
        val irohaAccounts =
            btcAddressesProvider.getRegisteredAccounts(tx.outputs)
        irohaAccounts.forEach { irohaAccount ->
            val event = SideChainEvent.PrimaryBlockChainEvent.OnPrimaryChainDeposit(
                tx.hashAsString,
                BigInteger.valueOf(tx.lockTime),
                irohaAccount,
                BTC_ASSET_NAME,
                BigInteger.valueOf(newBalance.getValue() - prevBalance.getValue()).toString(),
                ""
            )
            emitter.onNext(event)
        }
    }
}
