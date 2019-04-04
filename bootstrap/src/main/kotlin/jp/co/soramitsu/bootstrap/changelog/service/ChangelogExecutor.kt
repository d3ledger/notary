package jp.co.soramitsu.bootstrap.changelog.service

import jp.co.soramitsu.bootstrap.changelog.ChangelogAccountPublicInfo
import jp.co.soramitsu.bootstrap.changelog.ChangelogInterface
import jp.co.soramitsu.bootstrap.changelog.ChangelogPeer
import jp.co.soramitsu.bootstrap.changelog.history.ChangelogHistoryService
import jp.co.soramitsu.bootstrap.changelog.parser.ChangelogParser
import jp.co.soramitsu.bootstrap.dto.*
import jp.co.soramitsu.bootstrap.iroha.LazyIrohaAPIPool
import jp.co.soramitsu.bootstrap.iroha.sendBatchMST
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.File

/**
 * Executor of changelogs
 */
@Component
class ChangelogExecutor(
    @Autowired private val changelogParser: ChangelogParser,
    @Autowired private val irohaAPIPool: LazyIrohaAPIPool,
    @Autowired private val changelogHistoryService: ChangelogHistoryService
) {
    private val log = KLogging().logger
    private val irohaKeyRegexp = Regex("[A-Za-z0-9_]{1,64}")

    /**
     * Executes file based changelog
     * @param changelogRequest - request of changelog to execute
     */
    fun execute(changelogRequest: ChangelogFileRequest) {
        execute(changelogRequest.details, File(changelogRequest.changelogFile).readText())
    }

    /**
     * Executes script based changelog
     * @param changelogRequest - request of changelog to execute
     */
    fun execute(changelogRequest: ChangelogScriptRequest) {
        execute(changelogRequest.details, changelogRequest.script)
    }

    /**
     * Executes changelog
     * @param changelogRequestDetails - changelog request details(environment, project, keys and etc)
     * @param script - script to execute
     */
    private fun execute(changelogRequestDetails: ChangelogRequestDetails, script: String) {
        // Create Iroha API
        val irohaAPI = irohaAPIPool.getApi(changelogRequestDetails.irohaConfig)
        // Parse changelog script
        val changelog = changelogParser.parse(script)
        validateChangelog(changelog)
        val changelogTx = createChangelogTx(changelog, changelogRequestDetails)
        // Create changelog history tx
        val changelogHistoryTx =
            changelogHistoryService.createHistoryTx(
                changelog.schemaVersion,
                changelogTx.reducedHashHex
            )
        // Create changelog batch
        val changelogBatch = createChangelogBatch(changelogTx, changelogHistoryTx)
        // Sign changelog batch
        signBatch(changelogBatch, changelogRequestDetails.superuserKeys)
        // Send batch
        irohaAPI
            .sendBatchMST(changelogBatch.map { tx -> tx.build() }).fold(
                {
                    log.info {
                        "Changelog batch " +
                                "(schemaVersion:${changelog.schemaVersion}," +
                                " env:${changelogRequestDetails.meta.environment}," +
                                " project:${changelogRequestDetails.meta.project}," +
                                " irohaConfig:${changelogRequestDetails.irohaConfig})" +
                                " has been successfully sent"
                    }
                },
                { ex -> throw ex })
    }

    /**
     * Creates changelog tx
     * @param changelog - changelog script instance
     * @param changelogRequestDetails - details of changelog
     * @return changelog tx
     */
    private fun createChangelogTx(
        changelog: ChangelogInterface,
        changelogRequestDetails: ChangelogRequestDetails
    ): Transaction {
        return changelog.createChangelog(
            changelogRequestDetails.accounts.map { toChangelogAccount(it) },
            changelogRequestDetails.peers.map { toChangelogPeer(it) }
        )
    }

    /**
     * Creates changelog atomic batch
     * @param changelogTx - transaction with changelog
     * @param changelogHistoryTx - transaction with changelog history information
     */
    private fun createChangelogBatch(
        changelogTx: Transaction,
        changelogHistoryTx: Transaction
    ): List<Transaction> {
        return Utils.createTxUnsignedAtomicBatch(
            listOf(changelogTx, changelogHistoryTx)
        ).toList()
    }

    /**
     * Checks if changelog is valid
     * @param changelog - changelog to check
     * @throws IllegalArgumentException is changelog is not valid
     */
    private fun validateChangelog(changelog: ChangelogInterface) {
        if (!changelog.schemaVersion.matches(irohaKeyRegexp)) {
            throw IllegalArgumentException(
                "Changelog schema version '${changelog.schemaVersion}' is invalid. " +
                        "Must match regex $irohaKeyRegexp"
            )
        }
    }

    /**
     * Maps AccountPublicInfo to ChangelogAccountPublicInfo
     * @param accountPublicInfo - account info to map
     * @return ChangelogAccountPublicInfo
     */
    private fun toChangelogAccount(accountPublicInfo: AccountPublicInfo): ChangelogAccountPublicInfo {
        val changelogAccount = ChangelogAccountPublicInfo()
        changelogAccount.accountName = accountPublicInfo.accountName
        changelogAccount.domainId = accountPublicInfo.domainId
        changelogAccount.pubKeys = accountPublicInfo.pubKeys
        changelogAccount.quorum = accountPublicInfo.quorum
        return changelogAccount
    }

    /**
     * Maps Peer to ChangelogPeer
     * @param peer - peer to map
     * @return ChangelogPeer
     */
    private fun toChangelogPeer(peer: Peer): ChangelogPeer {
        val changelogPeer = ChangelogPeer()
        changelogPeer.hostPort = peer.hostPort
        changelogPeer.peerKey = peer.peerKey
        return changelogPeer
    }

    /**
     * Signs changelog batch
     * @param batch - changelog batch
     * @param superuserKeys - keys that are used to sign given batch
     */
    private fun signBatch(
        batch: List<Transaction>,
        superuserKeys: List<ClientKeyPair>
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
}
