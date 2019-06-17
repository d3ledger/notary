/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap.controller

import com.d3.eth.sidechain.util.DeployHelper
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
import org.web3j.crypto.*
import java.lang.IllegalArgumentException
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

                val ecKeyPairs = request.masterContract.notaries.map {
                    WalletUtils.loadCredentials(
                        it.password,
                        it.path
                    )
                }.map { it.ecKeyPair }

                if(ecKeyPairs.isEmpty()){
                    throw IllegalArgumentException("Provide paths to wallets of notaries, " +
                            "registered in smart contract for signature creation")
                }

                var addResult = true
                var removeResult = true
                var errorStatus: String? = null
                var errorMessage: String? = null

                if (request.removePeerAddress != null) {
                    val finalHash = prepareTrxHash(request.removePeerAddress)
                    val sigs = prepareSignatures(
                        request.masterContract.notaries.size,
                        ecKeyPairs,
                        finalHash
                    )
                    val trxResult = master.removePeerByPeer(
                        request.removePeerAddress,
                        defaultByteHash,
                        sigs.vv,
                        sigs.rr,
                        sigs.ss
                    ).send()
                    if (!trxResult.isStatusOK) {
                        errorStatus = trxResult.status
                        errorMessage =
                            "Error removeAddress action. Transaction hash is: trxResult.transactionHash"
                        removeResult = trxResult.isStatusOK
                    }
                }
                if (request.newPeerAddress != null) {
                    val finalHash = prepareTrxHash(request.newPeerAddress)
                    val sigs = prepareSignatures(
                        request.masterContract.notaries.size,
                        ecKeyPairs,
                        finalHash
                    )

                    val trxResult = master.addPeerByPeer(
                        request.newPeerAddress,
                        defaultByteHash,
                        sigs.vv,
                        sigs.rr,
                        sigs.ss
                    ).send()
                    if (!trxResult.isStatusOK) {
                        errorStatus = trxResult.status
                        errorMessage =
                            "Error addAddress action. Transaction hash is: trxResult.transactionHash"
                        addResult = trxResult.isStatusOK
                    }
                }
                val response = UpdateMasterContractResponse(addResult && removeResult)
                response.message = errorMessage
                response.errorCode = errorStatus
                return if (addResult && removeResult)
                    ResponseEntity.ok(response)
                else
                    ResponseEntity.status(HttpStatus.CONFLICT).body(response)
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

    private fun prepareTrxHash(removePeerAddress: String): String {
        return hashToAddAndRemovePeer(
            removePeerAddress,
            defaultIrohaHash
        )
    }

    @PostMapping("/deploy/D3/relayRegistry")
    fun deployRelayRegistry(@NotNull @RequestBody request: EthereumNetworkProperties): ResponseEntity<DeploySmartContractResponse> {
        return try {
            val deployHelper = createSmartContractDeployHelper(request)
            val relayRegistry = deployHelper.deployUpgradableRelayRegistrySmartContract()
            deployHelper.web3.shutdown()
            ResponseEntity.ok(DeploySmartContractResponse(relayRegistry.contractAddress))
        } catch (e: Exception) {
            log.error("Cannot deploy RelayRegistry smart contract", e)
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(DeploySmartContractResponse(e.javaClass.simpleName, e.message))
        }
    }

    @PostMapping("/deploy/D3/masterContract")
    fun deployMasterSmartContract(@NotNull @RequestBody request: DeployMasterContractRequest): ResponseEntity<DeployMasterContractResponse> {
        return try {
            val deployHelper = createSmartContractDeployHelper(request.network)
            val master =
                deployHelper.deployUpgradableMasterSmartContract(
                    request.relayRegistryAddress,
                    request.notaryEthereumAccounts
                )
            deployHelper.web3.shutdown()
            ResponseEntity.ok(
                DeployMasterContractResponse(
                    master.contractAddress,
                    master.xorTokenInstance().send()
                )
            )
        } catch (e: Exception) {
            log.error("Cannot deploy Master smart contract", e)
            val response = DeployMasterContractResponse()
            response.errorCode = e.javaClass.simpleName
            response.message = e.message
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(response)
        }
    }

    @PostMapping("/deploy/D3/relayImplementation")
    fun deployRelayImplementation(@NotNull @RequestBody request: DeployRelayImplementationRequest): ResponseEntity<DeploySmartContractResponse> {
        return try {
            val deployHelper = createSmartContractDeployHelper(request.network)
            val relayImplementation =
                deployHelper.deployRelaySmartContract(request.masterContractAddress)
            deployHelper.web3.shutdown()
            ResponseEntity.ok(DeploySmartContractResponse(relayImplementation.contractAddress))
        } catch (e: Exception) {
            log.error("Cannot deploy RelayImplementation smart contract", e)
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(DeploySmartContractResponse(e.javaClass.simpleName, e.message))
        }
    }

    @PostMapping("/deploy/D3/smartContracts")
    fun deployInitialSmartContracts(@NotNull @RequestBody request: AllInitialContractsRequest): ResponseEntity<DeployInitialContractsResponse> {
        log.info { "Run predeploy with notary addresses: ${request.notaryEthereumAccounts}" }
        return try {
            val deployHelper = createSmartContractDeployHelper(request.network)
            val relayRegistry = deployHelper.deployUpgradableRelayRegistrySmartContract()
            val master =
                deployHelper.deployUpgradableMasterSmartContract(
                    relayRegistry.contractAddress,
                    request.notaryEthereumAccounts
                )
            val relayImplementation = deployHelper.deployRelaySmartContract(master.contractAddress)
            deployHelper.web3.shutdown()
            return ResponseEntity.ok(
                DeployInitialContractsResponse(
                    master.contractAddress,
                    relayRegistry.contractAddress,
                    relayImplementation.contractAddress,
                    master.xorTokenInstance().send()
                )
            )
        } catch (e: Exception) {
            log.error("Cannot deploy smart contract", e)
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(DeployInitialContractsResponse(e.javaClass.simpleName, e.message))
        }
    }

    private fun createSmartContractDeployHelper(network: EthereumNetworkProperties): DeployHelper {
        return DeployHelperBuilder(
            network.ethereumConfig,
            network.ethPasswords
        ).setFastTransactionManager()
            .build()
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
        list.add("vacuum-service")
        return ResponseEntity.ok<List<String>>(list)
    }
}
