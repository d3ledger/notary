package provider.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import mu.KLogging
import org.bitcoinj.wallet.Wallet
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.util.ModelUtil
import util.getRandomId
import java.io.File

class BtcPublicKeyProvider(
    private val wallet: Wallet,
    private val walletFile: File,
    private val irohaConsumer: IrohaConsumer,
    private val btcRegistrationAccount: String
) {
    fun createKey(sessionAccountName: String): Result<Unit, Exception> {
        val key = wallet.freshReceiveKey()
        val pubKey = key.publicKeyAsHex
        return ModelUtil.setAccountDetail(
            irohaConsumer,
            btcRegistrationAccount,
            "$sessionAccountName@notary",
            String.getRandomId(),
            pubKey
        ).map {
            logger.info { "new pub key $pubKey was created" }
            wallet.saveToFile(walletFile)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
