package sidechain.iroha.util

import jp.co.soramitsu.iroha.ModelTransactionBuilder
import java.math.BigInteger

/**
 * TODO: Workaround, please remove as fast as binsings API improvement pr
 * will be merged into iroha (https://github.com/hyperledger/iroha/pull/1543)
 */
fun getHash(data: ByteArray): String {
    val tx = iroha.protocol.TransactionOuterClass.Transaction.parseFrom(data)
    val creator = tx.payload.reducedPayload.creatorAccountId
    val quorum = tx.payload.reducedPayload.quorum
    val time = tx.payload.reducedPayload.createdTime
    if (tx.payload.reducedPayload.commandsList.size != 1) {
        return ""
    }
    if (!tx.payload.reducedPayload.commandsList[0].hasTransferAsset()) {
        return ""
    }
    val cmd = tx.payload.reducedPayload.commandsList[0].transferAsset

    val hash = ModelTransactionBuilder()
            .creatorAccountId(creator)
            .createdTime(BigInteger.valueOf(time))
            .quorum(quorum)
            .transferAsset(
                    cmd.srcAccountId,
                    cmd.destAccountId,
                    cmd.assetId,
                    cmd.description,
                cmd.amount
            )
            .build()
            .hash()

    return hash.hex()
}
