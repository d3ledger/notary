package withdrawal.btc.transaction

import org.bitcoinj.core.Transaction

//Data class that holds transaction with its creation time
data class TimedTx(val creationTime: Long, val tx: Transaction) {
    companion object {
        fun create(tx: Transaction) = TimedTx(System.currentTimeMillis(), tx)
    }
}
