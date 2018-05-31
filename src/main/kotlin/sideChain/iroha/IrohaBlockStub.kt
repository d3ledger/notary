package sideChain.iroha

import notary.IrohaTransaction


data class IrohaSignature(val publicKey: ByteArray, val signature: ByteArray) {
    companion object {
        fun fromProto(bytes: ByteArray): IrohaSignature {
            val sig = iroha.protocol.Primitive.Signature.parseFrom(bytes)
            return IrohaSignature(sig.pubkey.toByteArray(), sig.signature.toByteArray())
        }
    }
}


/**
 * This implementation takes only AddPeer commands !!
 * TODO x3medima17, implement all commands
 *
 */
data class IrohaBlockStub(val height: Long,
                          val prevBlockHash: ByteArray,
                          val txNumber: Int,
                          val transactions: List<IrohaTransaction>,
                          val signatures: List<IrohaSignature>) {
    companion object {
        fun fromProto(bytes: ByteArray): IrohaBlockStub {

            val block = iroha.protocol.BlockOuterClass.Block.parseFrom(bytes)
            val payload = block.payload
            val height = payload.height
            val prevBlockHash = payload.prevBlockHash.toByteArray()
            val txNumber = payload.txNumber
            val transactions = payload.transactionsList.map { IrohaTransaction.fromProto(it.toByteArray()) }
            val signatures = block.signaturesList.map { IrohaSignature.fromProto(it.toByteArray()) }

            return IrohaBlockStub(height, prevBlockHash, txNumber, transactions, signatures)

        }
    }
}
