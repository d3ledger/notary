package jp.co.soramitsu.bootstrap.controller

import com.d3.eth.sidechain.util.DeployHelperBuilder
import jp.co.soramitsu.bootstrap.dto.EthWallet
import jp.co.soramitsu.bootstrap.dto.MasterContractResponse
import jp.co.soramitsu.bootstrap.dto.MasterContractsRequest
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.web3j.crypto.Keys
import org.web3j.crypto.Wallet
import org.web3j.crypto.WalletFile
import javax.validation.constraints.NotNull

@RestController
@RequestMapping("/eth")
class EthController {
    private val log = KLogging().logger

    @PostMapping("/deploy/D3/smartContracts")
    fun deployMasterSmartContracts(@NotNull @RequestBody request: MasterContractsRequest): ResponseEntity<MasterContractResponse> {

        log.info { "Run predeploy with notary addresses: ${request.notaryEthereumAccounts}" }
        var response: MasterContractResponse
        return if (request.network != null) {
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
            } catch(e:Exception) {
                log.error("Cannot deploy smart contract", e)
                response = MasterContractResponse(e.javaClass.simpleName, e.message)
            }
            ResponseEntity.status(HttpStatus.CONFLICT).body(response)
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(MasterContractResponse(HttpStatus.BAD_REQUEST.name, "Network parameters are not set"))
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
