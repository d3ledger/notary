package jp.co.soramitsu.bootstrap.changelog.service

import jp.co.soramitsu.bootstrap.changelog.ChangelogAccountPublicInfo
import jp.co.soramitsu.bootstrap.changelog.ChangelogPeer
import jp.co.soramitsu.bootstrap.changelog.parser.ChangelogParser
import jp.co.soramitsu.bootstrap.dto.*
import jp.co.soramitsu.bootstrap.iroha.LazyIrohaAPIPool
import jp.co.soramitsu.bootstrap.iroha.sendMST
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
    @Autowired private val irohaAPIPool: LazyIrohaAPIPool
) {
    private val log = KLogging().logger

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
        // Create changelog transaction
        val transaction = changelog.createChangelog(
            changelogRequestDetails.accounts.map { toChangelogAccount(it) },
            changelogRequestDetails.peers.map { toChangelogPeer(it) }
        )
        // Sign changelog transaction
        signTx(transaction, changelogRequestDetails.superuserKeys)
        // Send transaction
        irohaAPI.sendMST(transaction.build())
            .fold(
                { txHash -> log.info { "Changelog tx has been successfully sent. Tx hash $txHash" } },
                { ex -> throw ex })
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
     * Signs changelog transaction
     * @param tx - changelog transaction
     * @param superuserKeys - keys that are used to sign given batch
     */
    private fun signTx(
        tx: Transaction,
        superuserKeys: List<ClientKeyPair>
    ) {
        superuserKeys.map { clientKeyPair ->
            Utils.parseHexKeypair(
                clientKeyPair.publicKey,
                clientKeyPair.privateKey
            )
        }.forEach { keyPair ->
            tx.sign(keyPair)
        }
    }
}
