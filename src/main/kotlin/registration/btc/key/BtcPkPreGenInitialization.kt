package registration.btc.key

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import io.reactivex.Observable
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import jp.co.soramitsu.iroha.Keypair
import mu.KLogging
import org.bitcoinj.wallet.Wallet
import provider.btc.BtcPublicKeyProvider
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import java.io.File

class BtcPkPreGenInitialization(
    private val irohaKeypair: Keypair,
    private val btcPkPreGenConfig: BtcPkPreGenConfig
) {
    private val walletFile = File(btcPkPreGenConfig.btcWalletFilePath)
    private val wallet = Wallet.loadFromFile(walletFile)
    private val irohaConsumer = IrohaConsumerImpl(btcPkPreGenConfig.iroha)
    private val btcPublicKeyProvider =
        BtcPublicKeyProvider(
            wallet,
            walletFile,
            irohaConsumer,
            btcPkPreGenConfig.iroha.creator
        )

    fun init(): Result<Unit, Exception> {
        return IrohaChainListener(
            btcPkPreGenConfig.iroha.hostname,
            btcPkPreGenConfig.iroha.port,
            btcPkPreGenConfig.iroha.creator,
            irohaKeypair
        ).getBlockObservable().map { irohaObservable ->
            initIrohaObservable(irohaObservable)
        }
    }

    private fun initIrohaObservable(irohaObservable: Observable<BlockOuterClass.Block>) {
        irohaObservable.subscribe { block ->
            getSetDetailCommands(block).forEach { command ->
                if (command.setAccountDetail.accountId == btcPkPreGenConfig.pkTriggerAccount) {
                    val sessionAccountName = command.setAccountDetail.key
                    onGenerateKey(sessionAccountName).failure { ex -> logger.error("cannot generate key", ex) }
                }
            }
        }
    }

    private fun getSetDetailCommands(block: BlockOuterClass.Block): List<Commands.Command> {
        return block.payload.transactionsList.flatMap { tx -> tx.payload.reducedPayload.commandsList }
            .filter { command -> command.hasSetAccountDetail() }
    }

    private fun onGenerateKey(sessionAccountName: String): Result<Unit, Exception> {
        return btcPublicKeyProvider.createKey(sessionAccountName)
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
