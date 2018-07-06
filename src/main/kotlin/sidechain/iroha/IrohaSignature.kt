package sidechain.iroha

import java.util.*

/**
 * Class represents Signature from iroha model
 * @param publicKey signature's public key
 * @param signature the signature itself
 */
data class IrohaSignature(val publicKey: ByteArray, val signature: ByteArray) {

    override fun equals(other: Any?): Boolean {
        other as IrohaSignature
        return Arrays.equals(publicKey, other.publicKey) &&
                Arrays.equals(signature, other.signature)

    }

    override fun hashCode(): Int =
        Arrays.hashCode(publicKey + signature)
}
