package com.d3.commons.util

import java.security.PublicKey

/**
 * Extension function to convert PublicKey into hex representation
 */
fun PublicKey.toHexString() = this.encoded.joinToString("") { String.format("%02X", (it.toInt() and 0xFF)) }
