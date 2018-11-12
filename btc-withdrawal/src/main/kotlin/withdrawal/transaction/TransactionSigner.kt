package withdrawal.transaction

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import helper.address.getSignThreshold
import helper.address.outPutToBase58Address
import mu.KLogging
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.Utils
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import provider.btc.address.BtcRegisteredAddressesProvider
import util.hex

/*
   Class that is used to sign transactions using available private keys
 */
@Component
class TransactionSigner(
    @Autowired private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider
) {
    /**
     * Signs transaction using available private keys from wallet
     *
     * @param tx - transaction to sign
     * @param wallet - current wallet. Used to take private keys
     * @return - result with list full of signatures in form "input index"->"signatureHex hex"
     */
    fun sign(tx: Transaction, wallet: Wallet): Result<List<InputSignature>, Exception> {
        return Result.of { signUnsafe(tx, wallet) }
    }

    // Main signing function
    private fun signUnsafe(tx: Transaction, wallet: Wallet): List<InputSignature> {
        var inputIndex = 0
        val signatures = ArrayList<InputSignature>()
        tx.inputs.forEach { input ->
            getUsedPubKeys(outPutToBase58Address(input.connectedOutput!!)).fold({ pubKeys ->
                val keyPair = getPrivPubKeyPair(pubKeys, wallet)
                if (keyPair != null) {
                    //For redeem script
                    val ecPubKeys = pubKeys.map { pubKey -> ECKey.fromPublicOnly(Utils.parseAsHexOrBase58(pubKey)) }
                    val redeem = ScriptBuilder.createMultiSigOutputScript(getSignThreshold(pubKeys), ecPubKeys)
                    val hashOut = tx.hashForSignature(inputIndex, redeem, Transaction.SigHash.ALL, false)
                    val signature = keyPair.sign(hashOut)
                    signatures.add(InputSignature(inputIndex, String.hex(signature.encodeToDER())))
                    logger.info { "Tx ${tx.hashAsString} input $inputIndex was signed" }
                } else {
                    logger.warn { "Cannot sign ${tx.hashAsString} input $inputIndex" }
                }
            }, { ex ->
                throw IllegalStateException("Cannot get used pub keys for ${tx.hashAsString}", ex)
            })
            inputIndex++
        }
        return signatures
    }

    //Returns key pair related to one of given public keys. Returns null if no key pair was found
    private fun getPrivPubKeyPair(pubKeys: List<String>, wallet: Wallet): ECKey? {
        pubKeys.forEach { pubKey ->
            val ecKey = ECKey.fromPublicOnly(Utils.parseAsHexOrBase58(pubKey))
            val keyPair = wallet.findKeyFromPubHash(ecKey.pubKeyHash)
            if (keyPair != null) {
                return keyPair
            }
        }
        return null
    }

    // Returns public keys that were used in creation of given address
    private fun getUsedPubKeys(btcAddress: String): Result<List<String>, Exception> {
        return btcRegisteredAddressesProvider.getRegisteredAddresses()
            .map { registeredAddresses ->
                registeredAddresses.find { registeredAddress -> registeredAddress.address == btcAddress }!!.info.notaryKeys
            }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}

//Class that stores input with its signature in hex format
data class InputSignature(val index: Int, val signatureHex: String)
