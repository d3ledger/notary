package jp.co.soramitsu.notary.bootstrap.controller

import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.notary.bootstrap.dto.*
import jp.co.soramitsu.notary.bootstrap.genesis.GenesisInterface
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.xml.bind.DatatypeConverter

@RestController
@RequestMapping("/iroha")
class IrohaController(val genesisFactories: List<GenesisInterface>) {

    private val log = KLogging().logger

    @GetMapping("/config/accounts/{project}/{env}")
    fun getNeededAccounts(@PathVariable("project") project: String, @PathVariable("env") env: String): ResponseEntity<List<AccountPrototype>> {

        var genesisInfo: GenesisInterface? = null
        for (gf in genesisFactories) {
            if (gf.getProject().contentEquals(project) && gf.getEnvironment().contentEquals(env)) {
                genesisInfo = gf
            }
        }
        val accounts = ArrayList<AccountPrototype>()
        if (genesisInfo != null) {
            genesisInfo.getAccountsNeeded().forEach {
                if (!it.passive) {
                    accounts.add(it)
                }
            }
        }
        return ResponseEntity.ok<List<AccountPrototype>>(accounts)
    }

    @GetMapping("/projects/genesis")
    fun getProjects(): ResponseEntity<Map<String, String>> {
        val response = HashMap<String, String>()
        genesisFactories.forEach {
            response.putIfAbsent(it.getProject(), "")
            var value = response.get(it.getProject())
            response.put(
                it.getProject(),
                if (value!!.contentEquals("")) it.getEnvironment() else "$value:${it.getEnvironment()}"
            )
        }
        return ResponseEntity.ok<Map<String, String>>(response)
    }

    @GetMapping("/create/keyPair")
    fun generateKeyPair(): ResponseEntity<BlockchainCreds> {
        log.info("Request to generate KeyPair")

        val keyPair = Ed25519Sha3().generateKeypair()
        val response = BlockchainCreds(
            DatatypeConverter.printBase64Binary(keyPair.private.encoded),
            DatatypeConverter.printBase64Binary(keyPair.public.encoded)
        )
        return ResponseEntity.ok<BlockchainCreds>(response)
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
                    GenesisResponse(genesisFactory.createGenesisBlock(request.accounts, request.peers))
            } catch (e: Exception) {
                genesis = GenesisResponse()
                genesis.errorCode = e.javaClass.simpleName
                genesis.message = "Error happened for project:${request.meta.project} environment:${request.meta.environment}: ${e.message}"
            }
        } else {
            genesis = GenesisResponse()
            genesis.errorCode = "NO_GENESIS_FACTORY"
            genesis.message =
                "Genesis factory not found for project:${request.meta.project} environment:${request.meta.environment}"
        }
        return ResponseEntity.ok<GenesisResponse>(genesis)
    }
}

