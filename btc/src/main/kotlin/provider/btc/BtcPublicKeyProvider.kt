package provider.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.IrohaConfig
import model.IrohaCredential
import mu.KLogging
import org.bitcoinj.core.Address
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Utils
import org.bitcoinj.script.ScriptBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import provider.NotaryPeerListProvider
import provider.btc.network.BtcNetworkConfigProvider
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import util.getRandomId
import wallet.WalletFile

/**
 *  Bitcoin keys provider
 *  @param wallet - bitcoinJ wallet class
 *  @param walletFile - file where to save wallet
 *  @param irohaConfig - configutation to start Iroha client
 *  @param notaryPeerListProvider - class to query all current notaries
 */
@Component
class BtcPublicKeyProvider(
    //BTC wallet
    @Autowired private val walletFile: WalletFile,
    //Iroha configurations
    @Qualifier("btcPublicKeyProviderIrohaConfig")
    @Autowired private val irohaConfig: IrohaConfig,
    //Provider that helps us fetching all the peers registered in the network
    @Autowired private val notaryPeerListProvider: NotaryPeerListProvider,
    //BTC registration account
    @Qualifier("btcRegistrationCredential")
    @Autowired btcRegistrationCredential: IrohaCredential,
    //BTC registration account, that works in MST fashion
    @Qualifier("mstBtcRegistrationCredential")
    @Autowired private val mstBtcRegistrationCredential: IrohaCredential,
    //Notary account to store BTC addresses
    @Qualifier("notaryAccount")
    @Autowired private val notaryAccount: String,
    //Provider of network configurations
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider
) {

    private val sessionConsumer = IrohaConsumerImpl(btcRegistrationCredential, irohaConfig)
    private val multiSigConsumer = IrohaConsumerImpl(mstBtcRegistrationCredential, irohaConfig)

    init {
        logger.info { "BtcPublicKeyProvider was successfully initialized. Current wallet state:\n${walletFile.wallet}" }
    }

    /**
     * Creates notary public key and sets it into session account details
     * @param sessionAccountName - name of session account
     * @return new public key created by notary
     */
    fun createKey(sessionAccountName: String): Result<String, Exception> {
        // Generate new key from wallet
        val key = walletFile.wallet.freshReceiveKey()
        val pubKey = key.publicKeyAsHex
        return ModelUtil.setAccountDetail(
            sessionConsumer,
            "$sessionAccountName@btcSession",
            String.getRandomId(),
            pubKey
        ).map {
            walletFile.save()
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
                if (!walletFile.wallet.addWatchedAddress(msAddress)) {
                    throw IllegalStateException("BTC address $msAddress was not added to wallet")
                }
                logger.info { "Address $msAddress was added to wallet. Current wallet state:\n${walletFile.wallet}" }
                ModelUtil.setAccountDetail(
                    multiSigConsumer,
                    notaryAccount,
                    msAddress.toBase58(),
                    "free"
                ).fold({
                    //TODO this save will probably corrupt the wallet file
                    walletFile.save()
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
            walletFile.wallet.issuedReceiveKeys.find { ecKey -> ecKey.publicKeyAsHex == key } != null
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
        logger.info { "New BTC multisignature script $script" }
        return script.getToAddress(btcNetworkConfigProvider.getConfig())
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
