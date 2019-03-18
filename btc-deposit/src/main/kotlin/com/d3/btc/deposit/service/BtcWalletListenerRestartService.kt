package com.d3.btc.deposit.service

import com.d3.btc.deposit.config.BtcDepositConfig
import com.d3.btc.deposit.config.depositConfig
import com.d3.btc.deposit.handler.BtcDepositTxHandler
import com.d3.btc.deposit.listener.BtcConfirmedTxListener
import com.d3.btc.model.BtcAddress
import com.d3.btc.peer.SharedPeerGroup
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.commons.sidechain.SideChainEvent
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import io.reactivex.subjects.PublishSubject
import mu.KLogging
import org.bitcoinj.core.StoredBlock
import org.bitcoinj.core.Transaction
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutorService

/**
 * Service that is used to restart unconfirmed transactions confidence listeners
 */
@Component
class BtcWalletListenerRestartService(
    @Autowired private val btcDepositConfig: BtcDepositConfig,
    @Autowired private val confidenceListenerExecutorService: ExecutorService,
    @Autowired private val peerGroup: SharedPeerGroup,
    @Autowired private val btcEventsSource: PublishSubject<SideChainEvent.PrimaryBlockChainEvent>,
    @Autowired private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider
) {

    /**
     * Restarts unconfirmed transactions confidence listeners
     * @param transferWallet - wallet that stores all the D3 Bitcoin transactions. Used to get unconfirmed transactions
     * @param onTxSave - function that is called to save transaction in wallet
     */
    fun restartTransactionListeners(transferWallet: Wallet, onTxSave: () -> Unit): Result<Unit, Exception> {
        return btcRegisteredAddressesProvider.getRegisteredAddresses().map { registeredAddresses ->
            transferWallet.walletTransactions
                .filter { walletTransaction ->
                    val txDepth = walletTransaction.transaction.confidence.depthInBlocks
                    txDepth < btcDepositConfig.bitcoin.confidenceLevel
                }
                .map { walletTransaction ->
                    walletTransaction.transaction
                }
                .forEach { unconfirmedTx ->
                    logger.info { "Got unconfirmed transaction ${unconfirmedTx.hashAsString}. Try to restart listener." }
                    restartUnconfirmedTxListener(unconfirmedTx, registeredAddresses, onTxSave)
                }
        }
    }

    /**
     * Restarts unconfirmed transaction confidence listener
     * @param unconfirmedTx - transaction that needs confidence listener restart
     * @param registeredAddresses - registered addresses
     * @param onTxSave - function that is called to save transaction in wallet
     */
    private fun restartUnconfirmedTxListener(
        unconfirmedTx: Transaction,
        registeredAddresses: List<BtcAddress>,
        onTxSave: () -> Unit
    ) {
        // Get tx block hash
        unconfirmedTx.appearsInHashes?.let { appearsInHashes ->
            appearsInHashes.keys.firstOrNull()?.let { blockHash ->
                // Get tx block by hash
                peerGroup.getBlock(blockHash)?.let { block ->
                    // Create listener
                    unconfirmedTx.confidence.addEventListener(
                        confidenceListenerExecutorService,
                        createListener(unconfirmedTx, block, registeredAddresses, onTxSave)
                    )
                    logger.info("Listener for ${unconfirmedTx.hashAsString} has been restarted")
                }
            }
        }
    }

    /**
     * Creates unconfirmed transaction listener
     * @param unconfirmedTx - unconfirmed transaction which confidence will be listenable
     * @param block - block of transaction
     * @param registeredAddresses - registered addresses
     * @param onTxSave - function that is called to save transaction in wallet
     * @return restarted listener
     */
    private fun createListener(
        unconfirmedTx: Transaction,
        block: StoredBlock,
        registeredAddresses: List<BtcAddress>,
        onTxSave: () -> Unit
    )
            : BtcConfirmedTxListener {
        return BtcConfirmedTxListener(
            depositConfig.bitcoin.confidenceLevel,
            unconfirmedTx,
            block.header.time,
            BtcDepositTxHandler(
                registeredAddresses,
                btcEventsSource,
                onTxSave
            )::handleTx
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
