package jp.co.soramitsu.bootstrap.controller

import com.d3.eth.sidechain.util.DeployHelperBuilder
import com.d3.eth.sidechain.util.hashToAddAndRemovePeer
import jp.co.soramitsu.bootstrap.dto.*
import jp.co.soramitsu.bootstrap.utils.defaultByteHash
import jp.co.soramitsu.bootstrap.utils.defaultIrohaHash
import jp.co.soramitsu.bootstrap.utils.prepareSignatures
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.crypto.Wallet
import org.web3j.crypto.WalletFile
import java.math.BigInteger
import javax.validation.constraints.NotNull

@RestController
@RequestMapping("/eth")
class EthController {
    private val log = KLogging().logger

    @PostMapping("/deploy/D3/masterContract/update")
    fun addPeerToMasterContract(@NotNull @RequestBody request: UpdateMasterContractRequest): ResponseEntity<UpdateMasterContractResponse> {
        try {
            val deployHelper = DeployHelperBuilder(
                request.network.ethereumConfig,
                request.network.ethPasswords
            ).setFastTransactionManager()
                .build()
            if (request.masterContract.address != null) {
                val master = deployHelper.loadMasterContract(request.masterContract.address)
                if (request.newPeerAddress != null) {
                    val finalHash = hashToAddAndRemovePeer(
                        request.newPeerAddress,
                        defaultIrohaHash
                    )
                    val ecKeyPairs = request.masterContract.notaries.map {
                        ECKeyPair(
                            BigInteger(it.private),
                            BigInteger(it.public)
                        )
                    }
                    val sigs = prepareSignatures(request.masterContract.notaries.size, ecKeyPairs, finalHash)

                    val result = master.addPeerByPeer(
                        request.newPeerAddress,
                        defaultByteHash,
                        sigs.vv,
                        sigs.rr,
                        sigs.ss
                    ).send().isStatusOK
                    val response = UpdateMasterContractResponse(result)
                    return if (result)
                        ResponseEntity.ok(response)
                    else
                        ResponseEntity.status(HttpStatus.CONFLICT).body(response)
                }
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(UpdateMasterContractResponse(HttpStatus.BAD_REQUEST.name))
        } catch (e: Exception) {
            log.error("Error adding peer to smart contract", e)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                UpdateMasterContractResponse(e.javaClass.simpleName, e.message)
            )
        }
    }

    @PostMapping("/deploy/D3/smartContracts")
    fun deployMasterSmartContracts(@NotNull @RequestBody request: MasterContractsRequest): ResponseEntity<MasterContractResponse> {

        log.info { "Run predeploy with notary addresses: ${request.notaryEthereumAccounts}" }
        var response: MasterContractResponse
        try {
            val deployHelper = DeployHelperBuilder(
                request.network.ethereumConfig,
                request.network.ethPasswords
            ).setFastTransactionManager()
                .build()
            val relayRegistry = deployHelper.deployUpgradableRelayRegistrySmartContract()
            val master =
                deployHelper.deployUpgradableMasterSmartContract(
                    relayRegistry.contractAddress,
                    request.notaryEthereumAccounts
                )
            val relayImplementation = deployHelper.deployRelaySmartContract(master.contractAddress)
            response = MasterContractResponse(
                master.contractAddress,
                relayRegistry.contractAddress,
                relayImplementation.contractAddress,
                master.tokens.send()[0].toString()
            )
            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            log.error("Cannot deploy smart contract", e)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(MasterContractResponse(e.javaClass.simpleName, e.message))
        }
    }
    
    @GetMapping("/create/wallet")
    fun createWallet(@NotNull @RequestParam password: String): ResponseEntity<EthWallet> {
        try {
            val wallet: WalletFile = Wallet.createStandard(password, Keys.createEcKeyPair())
            return ResponseEntity.ok<EthWallet>(EthWallet(wallet))
        } catch (e: Exception) {
            log.error("Error creating Ethereum wallet", e)
            val response = EthWallet()
            response.errorCode = e.javaClass.simpleName
            response.message = e.message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }

    @GetMapping("/list/servicesWithWallet/d3/{peersCount}")
    fun listServiceEthWallets(@PathVariable("peersCount") peersCount: Int): ResponseEntity<List<String>> {
        val list = ArrayList<String>()
        var depositCounter = peersCount
        while (depositCounter > 0) {
            list.add("eth-deposit-service-peer$depositCounter")
            depositCounter--
        }
        list.add("eth-registration-service")
        list.add("eth-withdrawal-service")
        list.add("eth-genesis-wallet")
        return ResponseEntity.ok<List<String>>(list)
    }
}
