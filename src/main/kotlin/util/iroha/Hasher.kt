package util.iroha

import jp.co.soramitsu.iroha.ModelTransactionBuilder
import sidechain.iroha.util.toBigInteger
import java.math.BigInteger

/**
 * TODO: Workaround, please remove as fast as binsings API improvement pr
 * will be merged into iroha (https://github.com/hyperledger/iroha/pull/1543)
 */
fun getHash(data: ByteArray): String {
    val tx = iroha.protocol.BlockOuterClass.Transaction.parseFrom(data)
    val creator = tx.payload.creatorAccountId
    val quorum = tx.payload.quorum
    val time = tx.payload.createdTime
    if (tx.payload.commandsList.size != 1) {
        return ""
    }
    if (!tx.payload.commandsList[0].hasTransferAsset()) {
        return ""
    }
    val cmd = tx.payload.commandsList[0].transferAsset

    val hash = ModelTransactionBuilder()
            .creatorAccountId(creator)
            .createdTime(BigInteger.valueOf(time))
            .quorum(quorum.toLong())
            .transferAsset(
                    cmd.srcAccountId,
                    cmd.destAccountId,
                    cmd.assetId,
                    cmd.description,
                    cmd.amount.value.toBigInteger().toString()
            )
            .build()
            .hash()

    return hash.hex()
}