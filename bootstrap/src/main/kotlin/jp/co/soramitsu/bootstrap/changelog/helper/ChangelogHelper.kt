package jp.co.soramitsu.bootstrap.changelog.helper

import jp.co.soramitsu.bootstrap.changelog.ChangelogInterface
import jp.co.soramitsu.bootstrap.changelog.mapper.toChangelogAccount
import jp.co.soramitsu.bootstrap.changelog.mapper.toChangelogPeer
import jp.co.soramitsu.bootstrap.dto.AccountKeyPair
import jp.co.soramitsu.bootstrap.dto.ChangelogRequestDetails
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Query
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import java.security.KeyPair
import java.util.concurrent.atomic.AtomicLong

private val queryCounter = AtomicLong()

/**
 * Signs changelog batch
 * @param batch - changelog batch
 * @param superuserKeys - keys that are used to sign given batch
 */
fun signChangelogBatch(
    batch: List<Transaction>,
    superuserKeys: List<AccountKeyPair>
) {
    superuserKeys.map { clientKeyPair ->
        Utils.parseHexKeypair(
            clientKeyPair.publicKey,
            clientKeyPair.privateKey
        )
    }.forEach { keyPair ->
        batch.forEach { tx ->
            tx.sign(keyPair)
        }
    }
}

/**
 * Creates changelog atomic batch
 * @param changelogTx - transaction with changelog
 * @param changelogHistoryTx - transaction with changelog history information
 */
fun createChangelogBatch(
    changelogTx: Transaction,
    changelogHistoryTx: Transaction
): List<Transaction> {
    return Utils.createTxUnsignedAtomicBatch(
        listOf(changelogTx, changelogHistoryTx)
    ).toList()
}


/**
 * Creates changelog tx
 * @param changelog - changelog script instance
 * @param changelogRequestDetails - details of changelog
 * @return changelog tx
 */
fun createChangelogTx(
    changelog: ChangelogInterface,
    changelogRequestDetails: ChangelogRequestDetails
): Transaction {
    return changelog.createChangelog(
        changelogRequestDetails.accounts.map { toChangelogAccount(it) },
        changelogRequestDetails.peers.map { toChangelogPeer(it) }
    )
}

/**
 * Adds superuser quorum to tx
 * @param tx - transaction to modify
 * @param quorum - quorum value that will be set
 * @return modified tx
 */
fun addTxSuperuserQuorum(tx: Transaction, quorum: Int) = tx.makeMutable().setQuorum(quorum).build()

/**
 * Returns superuser account quorum
 * @param irohaAPI - Iroha API that is used to get superuser account quorum
 * @param superuserKeys - superuser keys that are used to query Iroha
 * @throws Exception if Iroha level error occurred
 * @return superuser quorum value
 */
fun getSuperuserQuorum(irohaAPI: IrohaAPI, superuserKeys: List<AccountKeyPair>): Int {
    // The first key pair must be enough to create Iroha query
    val firstKeyPair = superuserKeys.first()
    val superuserKeyPair =
        KeyPair(
            Utils.parseHexPublicKey(firstKeyPair.publicKey), Utils.parseHexPrivateKey(firstKeyPair.privateKey)
        )
    val query = Query.builder(ChangelogInterface.superuserAccountId, queryCounter.getAndIncrement())
        .getAccount(ChangelogInterface.superuserAccountId)
        .buildSigned(superuserKeyPair)

    val res = irohaAPI.query(query)
    // Check errors
    if (res.hasErrorResponse()) {
        throw Exception(
            "Cannot get ${ChangelogInterface.superuserAccountId} quorum. " +
                    "Error code ${res.errorResponse.errorCode} reason ${res.errorResponse.reason}"
        )
    }
    return res.accountResponse.account.quorum
}
