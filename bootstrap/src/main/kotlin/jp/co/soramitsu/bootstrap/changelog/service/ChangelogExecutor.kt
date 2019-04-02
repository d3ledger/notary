package jp.co.soramitsu.bootstrap.changelog.service

import jp.co.soramitsu.bootstrap.changelog.parser.ChangelogParser
import jp.co.soramitsu.bootstrap.dto.ChangelogFileRequest
import jp.co.soramitsu.bootstrap.dto.ChangelogRequestDetails
import jp.co.soramitsu.bootstrap.dto.ChangelogScriptRequest
import jp.co.soramitsu.bootstrap.dto.ClientKeyPair
import jp.co.soramitsu.bootstrap.iroha.LazyIrohaAPIPool
import jp.co.soramitsu.bootstrap.iroha.sendMSTBatch
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
        val transactions = changelog.createChangelog(
            changelogRequestDetails.accounts,
            changelogRequestDetails.peers
        )
        // Create batch
        val atomicBatch = Utils.createTxUnsignedAtomicBatch(transactions)
        // Sign changelog transactions
        signBatch(atomicBatch, changelogRequestDetails.superuserKeys)
        // Send transaction
        irohaAPI.sendMSTBatch(atomicBatch.map { batchTx -> batchTx.build() })
            .fold(
                { log.info { "Changelog has been successfully executed" } },
                { ex -> throw ex })
    }

    /**
     * Signs changelog batch
     * @param atomicBatch - changelog atomic batch
     * @param superuserKeys - keys that are used to sign given batch
     */
    private fun signBatch(
        atomicBatch: Iterable<Transaction>,
        superuserKeys: List<ClientKeyPair>
    ) {
        superuserKeys.map { clientKeyPair ->
            Utils.parseHexKeypair(
                clientKeyPair.publicKey,
                clientKeyPair.privateKey
            )
        }.forEach { keyPair ->
            atomicBatch.forEach { tx ->
                tx.sign(keyPair)
            }
        }
    }
}
