package com.d3.btc.helper.address

import org.bitcoinj.core.*
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptBuilder.createP2SHOutputScript


private const val UNDEFINED_ADDRESS = "[undefined]"
/**
 * Safely takes base58 encoded address from tx output
 *
 * @param output - tx output to take address from
 * @return - base58 encoded address or "[undefined]" if exception occurred
 */
fun outPutToBase58Address(output: TransactionOutput): String {
    try {
        val address = output.scriptPubKey?.getToAddress(output.params)?.toBase58()
        return if (address != null) {
            address
        } else {
            UNDEFINED_ADDRESS
        }
    } catch (expected: ScriptException) {
        return UNDEFINED_ADDRESS
    }
}

/**
 * Creates redeem script for MS address using given [pubKeys]
 * @param pubKeys - public keys that are used in MS address creation
 * @return redeem script
 */
fun createMsRedeemScript(pubKeys: List<String>): Script {
    val ecPubKeys = pubKeys.map { pubKey -> toEcPubKey(pubKey) }
    return ScriptBuilder.createRedeemScript(getSignThreshold(pubKeys), ecPubKeys)
}

/**
 * Creates MS address using given [notaryKeys]
 * @param notaryKeys - public keys that are used to create MS address
 * @param networkParameters - network parameters(RegTest, TestNet or MainNet)
 * @return MS script
 */
fun createMsAddress(notaryKeys: List<String>, networkParameters: NetworkParameters): Address {
    return createP2SHOutputScript(createMsRedeemScript(notaryKeys)).getToAddress(networkParameters)
}

/**
 * Creates EC pub key from hex
 * @param pubKey - public key in hex encoding
 * @return EC pub key
 */
fun toEcPubKey(pubKey: String) = ECKey.fromPublicOnly(Utils.parseAsHexOrBase58(pubKey))

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
