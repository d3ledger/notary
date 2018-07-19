package sidechain.iroha.util

import jp.co.soramitsu.iroha.ByteVector

/**
 * Extension function to convert [ByteArray] to [ByteVector]
 */
fun ByteArray.toByteVector(): ByteVector {
    val size = this.size
    val bs = ByteVector(size as Long)
    for (i in 0 until size) {
        bs[i] = this.get(i) as Short
    }
    return bs
}
