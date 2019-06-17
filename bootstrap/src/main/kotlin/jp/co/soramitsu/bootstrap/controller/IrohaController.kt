/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap.controller

import com.fasterxml.jackson.databind.ObjectMapper
import jp.co.soramitsu.bootstrap.dto.*
import jp.co.soramitsu.bootstrap.exceptions.ErrorCodes
import jp.co.soramitsu.bootstrap.genesis.GenesisInterface
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.xml.bind.DatatypeConverter

@RestController
@RequestMapping("/iroha")
class IrohaController(val genesisFactories: List<GenesisInterface>) {

    private val log = KLogging().logger
    private val mapper = ObjectMapper()

    @GetMapping("/config/accounts/{project}/{env}/{peersCount}")
    fun getNeededAccounts(
        @PathVariable("project") project: String,
        @PathVariable("env") env: String,
        @PathVariable("peersCount") peersCount: Int
    ): ResponseEntity<NeededAccountsResponse> {
        if (peersCount <= 0) {
            return ResponseEntity.ok<NeededAccountsResponse>(
                NeededAccountsResponse(
                    ErrorCodes.INCORRECT_PEERS_COUNT.name,
                    "peersCount path variable should exist with value >0"
                )
            )
        }

        val accounts = ArrayList<AccountPrototype>()

        genesisFactories.stream()
            .filter {
                it.getProject().contentEquals(project) && it.getEnvironment().contentEquals(
                    env
                )
            }
            .findAny()
            .ifPresent {
                accounts.addAll(it.getAccountsForConfiguration(peersCount))
            }

        return ResponseEntity.ok<NeededAccountsResponse>(NeededAccountsResponse(accounts))
    }

    @GetMapping("/projects/genesis")
    fun getProjects(): ResponseEntity<Projects> {
        try {
            val projMap = HashMap<String, ProjectInfo>()
            genesisFactories.forEach {
                if (!projMap.containsKey(it.getProject())) {
                    projMap.put(
                        it.getProject(),
                        ProjectInfo(it.getProject(), mutableListOf(it.getEnvironment()))
                    )
                } else {
                    projMap.get(it.getProject())?.environments?.add(it.getEnvironment())
                }
            }
            return ResponseEntity.ok<Projects>(Projects(projMap.values))
        } catch (e: Exception) {
            val response = Projects()
            response.errorCode = e.javaClass.simpleName
            response.message = e.message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }

    @GetMapping("/create/keyPair")
    fun generateKeyPair(): ResponseEntity<BlockchainCreds> {
        log.info("Request to generate KeyPair")
        try {
            val keyPair = Ed25519Sha3().generateKeypair()
            val response = BlockchainCreds(
                DatatypeConverter.printHexBinary(keyPair.private.encoded),
                DatatypeConverter.printHexBinary(keyPair.public.encoded)
            )
            return ResponseEntity.ok<BlockchainCreds>(response)
        } catch (e: Exception) {
            val response = BlockchainCreds()
            response.errorCode = e.javaClass.simpleName
            response.message = e.message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }

    @PostMapping("/create/genesisBlock")
    fun generateGenericBlock(@RequestBody request: GenesisRequest): ResponseEntity<GenesisResponse> {
        val conflict = isValidRequest(request)
        if (conflict != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(GenesisResponse(conflict.errorCode, conflict.message))
        }
        log.info("Request of genesis block")
        val genesisFactory = genesisFactories.stream().filter {
            it.getProject().contentEquals(request.meta.project)
                    && it.getEnvironment().contentEquals(request.meta.environment)
        }.findAny().orElse(null)
        val genesis: GenesisResponse
        if (genesisFactory != null) {
            try {
                genesis =
                    GenesisResponse(
                        genesisFactory.createGenesisBlock(
                            request.accounts,
                            request.peers
                        )
                    )
            } catch (e: Exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    GenesisResponse(
                        e.javaClass.simpleName,
                        "Error happened for project:${request.meta.project} environment:${request.meta.environment}: ${e.message}"
                    )
                )
            }
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                GenesisResponse(
                    ErrorCodes.NO_GENESIS_FACTORY.name,
                    "Genesis factory not found for project:${request.meta.project} environment:${request.meta.environment}"
                )
            )
        }
        return ResponseEntity.ok<GenesisResponse>(genesis)
    }

    private fun isValidRequest(request: GenesisRequest): Conflictable? {

        val result = request.peers.filter { it.peerKey.isEmpty() }.toList()

        if (result.isNotEmpty()) {
            var message = "Peers with empty publicKeys:"
            result.forEach {
                message += " " + it.hostPort
            }
            return Conflictable(ErrorCodes.EMPTY_PEER_PUBLIC_KEY.name, "")
        }
        return null
    }
}

