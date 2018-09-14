package provider.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import mu.KLogging
import org.bitcoinj.core.Address
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Utils
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.wallet.Wallet
import provider.NotaryPeerListProvider
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.util.ModelUtil
import util.getRandomId
import java.io.File

class BtcPublicKeyProvider(
    //BTC wallet
    private val wallet: Wallet,
    //BTC wallet file storage
    private val walletFile: File,
    private val irohaConsumer: IrohaConsumer,
    //Provider that helps us fetching all the peers registered in the network
    private val notaryPeerListProvider: NotaryPeerListProvider,
    //BTC registration account
    private val btcRegistrationAccount: String,
    //BTC registration account, that works in MST fashion
    private val mstBtcRegistrationAccount: String,
    //Notary account to store BTC addresses
    private val notaryAccount: String
) {

    /**
     * Creates public key and sets it into session account details
     * @param sessionAccountName - name of session account
     * @return Result of operation
     */
    fun createKey(sessionAccountName: String): Result<String, Exception> {
        val key = wallet.freshReceiveKey()
        val pubKey = key.publicKeyAsHex
        return ModelUtil.setAccountDetail(
            irohaConsumer,
            btcRegistrationAccount,
            "$sessionAccountName@btcSession",
            String.getRandomId(),
            pubKey
        ).map {
            wallet.saveToFile(walletFile)
            pubKey
        }
    }

    /**
     * Creates multisignature address if enough public keys are provided
     * @param notaryKeys - public keys of notaries
     * @return Result of operation
     */
    fun checkAndCreateMsAddress(notaryKeys: Collection<String>): Result<Unit, Exception> {
        return Result.of {
            val peers = notaryPeerListProvider.getPeerList().size
            if (peers == 0) {
                throw IllegalStateException("No peers to create btc multisignature address")
            } else if (notaryKeys.size == peers && hasMyKey(notaryKeys)) {
                val threshold = getThreshold(peers)
                val msAddress = createMsAddress(notaryKeys, threshold)
                wallet.addWatchedAddress(msAddress)
                ModelUtil.setAccountDetail(
                    irohaConsumer,
                    mstBtcRegistrationAccount,
                    notaryAccount,
                    msAddress.toBase58(),
                    "free"
                ).fold({
                    wallet.saveToFile(walletFile)
                    logger.info { "New BTC multisignature address $msAddress was created " }
                }, { ex -> throw ex })
            }
        }
    }

    /**
     * Checks if current notary has its key in notaryKeys
     * @param notaryKeys - public keys of notaries
     * @return true if at least one current notary key is among given notaryKeys
     */
    private fun hasMyKey(notaryKeys: Collection<String>): Boolean {
        val hasMyKey = notaryKeys.find { key ->
            wallet.issuedReceiveKeys.find { ecKey -> ecKey.publicKeyAsHex == key } != null
        } != null
        return hasMyKey
    }

    /**
     * Creates multisignature address
     * @param notaryKeys - public keys of notaries
     * @return created address
     */
    private fun createMsAddress(notaryKeys: Collection<String>, threshold: Int): Address {
        val keys = ArrayList<ECKey>()
        notaryKeys.forEach { key ->
            val ecKey = ECKey.fromPublicOnly(Utils.parseAsHexOrBase58(key))
            keys.add(ecKey)
        }
        val script = ScriptBuilder.createP2SHOutputScript(threshold, keys)
        return script.getToAddress(RegTestParams.get())
    }

    private fun getThreshold(peers: Int): Int {
        return (peers * 2 / 3) + 1;
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
