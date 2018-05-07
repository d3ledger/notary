package sideChain.iroha.util

import ByteVector

/**
 * Extension function to convert [ByteVector] to [ByteArray]
 */
fun ByteVector.toByteArray(): ByteArray {
    val size = this.size().toInt()
    val bs = ByteArray(size)
    for (i in 0 until size) {
        bs[i] = this.get(i).toByte()
    }
    return bs
}
