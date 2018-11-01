package withdrawal.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import healthcheck.HealthyService
import helper.network.getPeerGroup
import helper.network.startChainDownload
import io.reactivex.schedulers.Schedulers
import iroha.protocol.Commands
import mu.KLogging
import org.bitcoinj.core.Transaction
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import provider.btc.network.BtcNetworkConfigProvider
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.util.getTransferCommands
import withdrawal.btc.config.BtcWithdrawalConfig
import withdrawal.transaction.TransactionCreator
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/*
    Class that initiates listeners that will be used to handle Bitcoin withdrawal logic
 */
@Component
class BtcWithdrawalInitialization(
    @Autowired private val btcWithdrawalConfig: BtcWithdrawalConfig,
    @Autowired private val irohaChainListener: IrohaChainListener,
    @Autowired private val transactionCreator: TransactionCreator,
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider
) : HealthyService() {

    //TODO create rollback mechanism
    private val unsignedTransactions = ConcurrentHashMap<String, Transaction>()

    fun init(): Result<Unit, Exception> {
        val wallet = Wallet.loadFromFile(File(btcWithdrawalConfig.bitcoin.walletPath))
        return initTransferListener(wallet, irohaChainListener)
            .flatMap { initBtcBlockChain(wallet) }
    }

    /**
     * Initiates listener that listens to withdrawal events in Iroha
     * @param irohaChainListener - listener of Iroha blockchain
     * @return result of initiation process
     */
    private fun initTransferListener(wallet: Wallet, irohaChainListener: IrohaChainListener): Result<Unit, Exception> {
        return irohaChainListener.getBlockObservable().map { irohaObservable ->
            irohaObservable.subscribeOn(Schedulers.from(Executors.newSingleThreadExecutor()))
                .subscribe({ block ->
                    getTransferCommands(block).forEach { command ->
                        handleTransferCommand(
                            wallet,
                            command.transferAsset
                        )
                    }
                }, { ex ->
                    notHealthy()
                    logger.error("Error on transfer events subscription", ex)
                })
            logger.info { "Iroha transfer events listener was initialized" }
            Unit
        }
    }

    /**
     * Starts Bitcoin block chain download process
     * @param wallet - wallet object that will be enriched with block chain data: sent, unspent transactions, last processed block and etc
     */
    private fun initBtcBlockChain(wallet: Wallet): Result<Unit, Exception> {
        return Result.of {
            getPeerGroup(
                wallet,
                btcNetworkConfigProvider.getConfig(),
                btcWithdrawalConfig.bitcoin.blockStoragePath
            )
        }.map { peerGroup -> startChainDownload(peerGroup, btcWithdrawalConfig.bitcoin.host) }
    }

    /**
     * Handles "transfer asset" command
     * @param transferCommand - object with "transfer asset" data: source account, destination account, amount and etc
     */
    private fun handleTransferCommand(wallet: Wallet, transferCommand: Commands.TransferAsset) {
        if (transferCommand.destAccountId != btcWithdrawalConfig.withdrawalCredential.accountId) {
            return
        }
        logger.info {
            "Withdrawal event(" +
                    "from:${transferCommand.srcAccountId} " +
                    "to:${transferCommand.description} " +
                    "amount:${transferCommand.amount})"
        }
        //TODO check if destination address from 'transferCommand.description' is whitelisted
        transactionCreator.createTransaction(
            wallet,
            transferCommand.amount.toLong(),
            transferCommand.description,
            btcWithdrawalConfig.bitcoin.confidenceLevel
        ).fold({ transaction ->
            val txHash = transaction.hashAsString
            unsignedTransactions[txHash] = transaction
            logger.info { "Tx $txHash was added to collection of unsigned transactions" }
            //TODO start collecting signatures
            Unit
        }, { ex -> logger.error("Cannot create withdrawal transaction", ex) })
    }

    fun getUnsignedTransactions() = unsignedTransactions.values

    /**
     * Logger
     */
    companion object : KLogging()

}
