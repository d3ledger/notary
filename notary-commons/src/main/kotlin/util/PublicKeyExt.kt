package util

import java.security.PublicKey

/**
 * Extenstion function to convert PublicKey into hex representation
 */
fun PublicKey.toHexString() = this.encoded.joinToString("") { String.format("%02X", (it.toInt() and 0xFF)) }