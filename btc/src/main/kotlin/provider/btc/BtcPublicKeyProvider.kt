package provider.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.IrohaConfig
import model.IrohaCredential
import mu.KLogging
import org.bitcoinj.core.Address
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Utils
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.wallet.Wallet
import provider.NotaryPeerListProvider
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import util.getRandomId
import java.io.File

/**
 *  Bitcoin keys provider
 *  @param wallet - bitcoinJ wallet class
 *  @param walletFile - file where to save wallet
 *  @param irohaConfig - configutation to start Iroha client
 *  @param notaryPeerListProvider - class to query all current notaries
 */
class BtcPublicKeyProvider(
    //BTC wallet
    private val wallet: Wallet,
    //BTC wallet file storage
    private val walletFile: File,
    private val irohaConfig: IrohaConfig,
    //Provider that helps us fetching all the peers registered in the network
    private val notaryPeerListProvider: NotaryPeerListProvider,
    //BTC registration account
    btcRegistrationCredential: IrohaCredential,
    //BTC registration account, that works in MST fashion
    mstBtcRegistrationCredential: IrohaCredential,
    //Notary account to store BTC addresses
    private val notaryAccount: String
) {

    private val sessionConsumer = IrohaConsumerImpl(btcRegistrationCredential, irohaConfig)
    private val multiSigConsumer = IrohaConsumerImpl(mstBtcRegistrationCredential, irohaConfig)

    /**
     * Creates notary public key and sets it into session account details
     * @param sessionAccountName - name of session account
     * @return new public key created by notary
     */
    fun createKey(sessionAccountName: String): Result<String, Exception> {
        // Generate new key from wallet
        val key = wallet.freshReceiveKey()
        val pubKey = key.publicKeyAsHex
        return ModelUtil.setAccountDetail(
            sessionConsumer,
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
     * @param notaryKeys - list of all notaries public keys
     * @return Result of operation
     */
    fun checkAndCreateMultiSigAddress(notaryKeys: Collection<String>): Result<Unit, Exception> {
        return Result.of {
            val peers = notaryPeerListProvider.getPeerList().size
            if (peers == 0) {
                throw IllegalStateException("No peers to create btc multisignature address")
            } else if (notaryKeys.size == peers && hasMyKey(notaryKeys)) {
                val threshold = getThreshold(peers)
                val msAddress = createMsAddress(notaryKeys, threshold)
                wallet.addWatchedAddress(msAddress)
                ModelUtil.setAccountDetail(
                    multiSigConsumer,
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
     * Creates multi signature bitcoin address
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

    /**
     * Calculate threshold
     * @param peers - total number of peers
     * @return minimal number of signatures required
     */
    private fun getThreshold(peers: Int): Int {
        return (peers * 2 / 3) + 1;
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
