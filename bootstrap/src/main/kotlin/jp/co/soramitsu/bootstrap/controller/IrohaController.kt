package jp.co.soramitsu.bootstrap.controller

import jp.co.soramitsu.bootstrap.dto.*
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

    @GetMapping("/config/accounts/{project}/{env}")
    fun getNeededAccounts(@PathVariable("project") project: String, @PathVariable("env") env: String): ResponseEntity<List<AccountPrototype>> {

        val accounts = ArrayList<jp.co.soramitsu.bootstrap.dto.AccountPrototype>()

        genesisFactories.stream()
            .filter {
                it.getProject().contentEquals(project) && it.getEnvironment().contentEquals(
                    env
                )
            }
            .findAny()
            .ifPresent {
                it.getAccountsNeeded()
                    .filter { !it.passive }
                    .forEach { accounts.add(it) }
            }
        return ResponseEntity.ok<List<AccountPrototype>>(accounts)
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
        } catch(e:Exception) {
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

            log.info("Request of genesis block")
            val genesisFactory = genesisFactories.filter {
                it.getProject().contentEquals(request.meta.project)
                        && it.getEnvironment().contentEquals(request.meta.environment)
            }.firstOrNull()
            var genesis: GenesisResponse
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
                    genesis = GenesisResponse()
                    genesis.errorCode = e.javaClass.simpleName
                    genesis.message =
                        "Error happened for project:${request.meta.project} environment:${request.meta.environment}: ${e.message}"
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(genesis)
                }
            } else {
                genesis = GenesisResponse()
                genesis.errorCode = "NO_GENESIS_FACTORY"
                genesis.message =
                    "Genesis factory not found for project:${request.meta.project} environment:${request.meta.environment}"
                return ResponseEntity.status(HttpStatus.CONFLICT).body(genesis)
            }
            return ResponseEntity.ok<GenesisResponse>(genesis)
        }
    }

