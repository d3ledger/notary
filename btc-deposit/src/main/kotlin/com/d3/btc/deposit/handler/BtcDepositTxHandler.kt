package com.d3.btc.deposit.handler

import com.d3.btc.helper.address.outPutToBase58Address
import com.d3.btc.helper.currency.satToBtc
import com.d3.btc.model.BtcAddress
import com.d3.commons.sidechain.SideChainEvent
import io.reactivex.subjects.PublishSubject
import mu.KLogging
import org.bitcoinj.core.Transaction
import java.math.BigInteger
import java.util.*

private const val BTC_ASSET_NAME = "btc"
private const val BTC_ASSET_DOMAIN = "bitcoin"
private const val TWO_HOURS_MILLIS = 2 * 60 * 60 * 1000L

/**
 * Handler of Bitcoin deposit transactions
 * @param registeredAddresses - registered addresses. Used to get Bitcoin address data
 * @param btcEventsSource - source of Bitcoin deposit events
 * @param onTxSave - function that is called to save transaction in wallet
 */
class BtcDepositTxHandler(
    private val registeredAddresses: List<BtcAddress>,
    private val btcEventsSource: PublishSubject<SideChainEvent.PrimaryBlockChainEvent>,
    private val onTxSave: () -> Unit
) {

    /**
     * Handles deposit transaction
     * @param tx - Bitcoin deposit transaction
     * @param blockTime - time of block where [tx] appeared for the first time. This time is used in MST
     */
    fun handleTx(tx: Transaction, blockTime: Date) {
        tx.outputs.forEach { output ->
            val txBtcAddress = outPutToBase58Address(output)
            logger.info { "Tx ${tx.hashAsString} has output address $txBtcAddress" }
            val btcAddress = registeredAddresses.firstOrNull { btcAddress -> btcAddress.address == txBtcAddress }
            if (btcAddress != null) {
                val btcValue = satToBtc(output.value.value)
                val event = SideChainEvent.PrimaryBlockChainEvent.OnPrimaryChainDeposit(
                    tx.hashAsString,
                    /*
                    Due to Iroha time restrictions, tx time must be in range [current time - 1 day; current time + 5 min],
                    while Bitcoin block time must be in range [median time of last 11 blocks; network time + 2 hours].
                    Given these restrictions, block time may be more than 5 minutes ahead of current time.
                    Subtracting 2 hours is just a simple workaround of this problem.
                    */
                    BigInteger.valueOf(blockTime.time - TWO_HOURS_MILLIS),
                    btcAddress.info.irohaClient!!,
                    "$BTC_ASSET_NAME#$BTC_ASSET_DOMAIN",
                    btcValue.toPlainString(),
                    ""
                )
                logger.info {
                    "BTC deposit event(tx ${tx.hashAsString}, amount ${btcValue.toPlainString()}) was created. " +
                            "Related client is ${btcAddress.info.irohaClient}. "
                }
                btcEventsSource.onNext(event)
                //TODO better call this function after event consumption.
                onTxSave()
            }
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
