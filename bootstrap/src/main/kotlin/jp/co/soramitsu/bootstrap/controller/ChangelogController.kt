package jp.co.soramitsu.bootstrap.controller

import jp.co.soramitsu.bootstrap.changelog.service.ChangelogExecutor
import jp.co.soramitsu.bootstrap.dto.ChangelogFileRequest
import jp.co.soramitsu.bootstrap.dto.ChangelogRequestDetails
import jp.co.soramitsu.bootstrap.dto.ChangelogScriptRequest
import jp.co.soramitsu.bootstrap.dto.Conflictable
import jp.co.soramitsu.bootstrap.exceptions.ErrorCodes
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/changelog")
class ChangelogController(private val changelogExecutor: ChangelogExecutor) {

    private val log = KLogging().logger

    @PostMapping("/execute/changelogScript")
    fun executeScriptChangelog(@RequestBody request: ChangelogScriptRequest): ResponseEntity<Conflictable> {
        return checkRequestAndExecute(request.details) { changelogExecutor.execute(request) }
    }

    @PostMapping("/execute/changelogFile")
    fun executeFileChangelog(@RequestBody request: ChangelogFileRequest): ResponseEntity<Conflictable> {
        return checkRequestAndExecute(request.details) { changelogExecutor.execute(request) }
    }

    /**
     * Checks and executes changelog
     * @param request - changelog request to check
     * @param executor - execution logic
     */
    private fun checkRequestAndExecute(
        request: ChangelogRequestDetails,
        executor: () -> Unit
    ): ResponseEntity<Conflictable> {
        val conflict = isValidChangelogRequest(request)
        if (conflict != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(conflict)
        }
        return try {
            executor()
            ResponseEntity.status(HttpStatus.OK).build()
        } catch (e: Exception) {
            log.error("Cannot execute changelog", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                Conflictable(
                    e.javaClass.simpleName,
                    "Error happened for project:${request.meta.project} " +
                            "environment:${request.meta.environment}: ${e.message}"
                )
            )
        }
    }
}

/**
 * Checks if changelog request is valid
 * @param request - request to check
 * @return [Conflictable] object if request is not valid, otherwise null
 */
private fun isValidChangelogRequest(request: ChangelogRequestDetails): Conflictable? {
    val emptyPeers = request.peers.filter { peer -> peer.peerKey.isEmpty() }
    if (emptyPeers.isNotEmpty()) {
        val message = "Peers with empty publicKeys:" + emptyPeers.map { emptyPeer -> emptyPeer.hostPort }
        return Conflictable(ErrorCodes.EMPTY_PEER_PUBLIC_KEY.name, message)
    }
    val emptyAccounts = request.accounts.filter { account -> account.pubKeys.any { key -> key.isEmpty() } }
    if (emptyAccounts.isNotEmpty()) {
        val message =
            "Accounts with empty publicKeys:" + emptyAccounts.map { it.accountName }
        return Conflictable(ErrorCodes.EMPTY_ACCOUNT_PUBLIC_KEY.name, message)
    }
    if (request.superuserKeys.isEmpty()) {
        val message = "Empty superuser keys"
        return Conflictable(ErrorCodes.EMPTY_SUPER_USER_KEYS.name, message)
    }
    return null
}
