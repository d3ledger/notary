package jp.co.soramitsu.bootstrap.changelog.service

import jp.co.soramitsu.bootstrap.changelog.ChangelogInterface
import jp.co.soramitsu.bootstrap.changelog.helper.*
import jp.co.soramitsu.bootstrap.changelog.history.ChangelogHistoryService
import jp.co.soramitsu.bootstrap.changelog.parser.ChangelogParser
import jp.co.soramitsu.bootstrap.dto.ChangelogFileRequest
import jp.co.soramitsu.bootstrap.dto.ChangelogRequestDetails
import jp.co.soramitsu.bootstrap.dto.ChangelogScriptRequest
import jp.co.soramitsu.bootstrap.iroha.LazyIrohaAPIPool
import jp.co.soramitsu.bootstrap.iroha.sendBatchMST
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
        val superuserQuorum = getSuperuserQuorum(irohaAPI, changelogRequestDetails.superuserKeys)
        // Create changelog tx
        val changelogTx = addTxSuperuserQuorum(
            createChangelogTx(changelog, changelogRequestDetails),
            superuserQuorum
        )
        // Create changelog history tx
        val changelogHistoryTx = addTxSuperuserQuorum(
            changelogHistoryService.createHistoryTx(
                changelog.schemaVersion,
                changelogTx.reducedHashHex
            ), superuserQuorum
        )
        // Create changelog batch
        val changelogBatch = createChangelogBatch(changelogTx, changelogHistoryTx)
        // Sign changelog batch
        signChangelogBatch(changelogBatch, changelogRequestDetails.superuserKeys)
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
}
