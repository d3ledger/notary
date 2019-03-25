/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap.controller

import jp.co.soramitsu.bootstrap.dto.EthWallet
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.web3j.crypto.Keys
import org.web3j.crypto.Wallet
import org.web3j.crypto.WalletFile

@RestController
@RequestMapping("/eth")
class EthController {
    private val log = KLogging().logger

    @GetMapping("/create/wallet")
    fun createWallet(): ResponseEntity<EthWallet> {
        try {
            val wallet: WalletFile = Wallet.createStandard("AB", Keys.createEcKeyPair())
            return ResponseEntity.ok<EthWallet>(EthWallet(wallet))
        } catch(e:Exception) {
            log.error("Error creating Ethereum wallet",e)
            val response = EthWallet()
            response.errorCode = e.javaClass.simpleName
            response.message = e.message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }

    @GetMapping("/list/servicesWithWallet/d3/{peersCount}")
    fun listServicesWithEthWallet(@PathVariable("peersCount")peersCount: Int): ResponseEntity<List<String>> {
        val list = ArrayList<String>()
        var depositCounter = peersCount
        while(depositCounter > 0) {
            list.add("eth-deposit-service-peer$peersCount")
            depositCounter--
        }
        list.add("eth-registration-service")
        list.add("eth-withdrawal-service")
        list.add("eth-genesis-wallet")
        return ResponseEntity.ok<List<String>>(list)
    }
}
