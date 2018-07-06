package sidechain.iroha

import notary.IrohaTransaction
import java.util.*

/**
 * This implementation takes only AddPeer commands !!
 * TODO x3medima17, implement all commands
 *
 */
data class IrohaBlockStub(
    val height: Long,
    val prevBlockHash: ByteArray,
    val txNumber: Int,
    val transactions: List<IrohaTransaction>,
    val signatures: List<IrohaSignature>
) {

    override fun equals(other: Any?): Boolean {
        other as IrohaBlockStub
        return height == other.height &&
                Arrays.equals(prevBlockHash, other.prevBlockHash) &&
                txNumber == other.txNumber &&
                transactions == other.transactions &&
                signatures == other.signatures
    }

    override fun hashCode() =
        listOf(
            height.hashCode(),
            Arrays.hashCode(prevBlockHash),
            txNumber,
            transactions.hashCode(),
            signatures.hashCode()
        ).hashCode()

}
