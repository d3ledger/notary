package helper.address

import org.bitcoinj.core.AddressFormatException
import org.bitcoinj.core.Base58
import org.bitcoinj.core.ScriptException
import org.bitcoinj.core.TransactionOutput

/**
 * Safely takes base58 encoded address from tx output
 *
 * @param output - tx output to take address from
 * @return - base58 encoded address or "[undefined]" if exception occurred
 */
fun outPutToBase58Address(output: TransactionOutput): String {
    try {
        return output.scriptPubKey.getToAddress(output.params).toBase58()
    } catch (expected: ScriptException) {
        return "[undefined]"
    }
}

/**
 * Calculate threshold for signing
 * @param peers - total number of peers
 * @return minimal number of signatures required
 */
fun getSignThreshold(peers: Int): Int {
    return (peers * 2 / 3) + 1
}

/**
 * Calculate threshold for signing
 * @param pubKeys - used keys
 * @return minimal number of signatures required
 */
fun getSignThreshold(pubKeys: List<String>): Int {
    return getSignThreshold(pubKeys.size)
}

/**
 * Checks if address is valid base58 Bitcoin address
 * @param address - address to check
 * @return 'true' if address is valid base58 address
 */
fun isValidBtcAddress(address: String): Boolean {
    try {
        Base58.decodeChecked(address)
        return true
    } catch (expected: AddressFormatException) {
        return false;
    }
}
