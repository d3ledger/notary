package registration.btc

import com.github.kittinunf.result.Result
import org.bitcoinj.wallet.Wallet
import registration.IrohaAccountCreator
import registration.RegistrationStrategy
import sidechain.iroha.consumer.IrohaConsumer
import java.io.File

class BtcRegistrationStrategyImpl(
    irohaConsumer: IrohaConsumer,
    notaryIrohaAccount: String,
    creator: String,
    walletFilePath: String
) : RegistrationStrategy {
    private val walletFile = File(walletFilePath)
    private val wallet = Wallet.loadFromFile(walletFile)

    private val irohaAccountCreator = IrohaAccountCreator(irohaConsumer, notaryIrohaAccount, creator, "bitcoin_wallet")

    override fun register(name: String, pubkey: String): Result<String, Exception> {
        val btcAddress = wallet.freshReceiveAddress().toString()
        wallet.saveToFile(walletFile)
        return irohaAccountCreator.create(btcAddress, name, pubkey)
    }
}
