package helper.address

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
