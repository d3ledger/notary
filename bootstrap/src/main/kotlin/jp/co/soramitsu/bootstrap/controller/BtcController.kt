/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap.controller

import jp.co.soramitsu.bootstrap.dto.BtcNetwork
import jp.co.soramitsu.bootstrap.dto.BtcWallet
import mu.KLogging
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayOutputStream
import javax.xml.bind.DatatypeConverter

@RestController
@RequestMapping("/btc")
@PropertySource("classpath:application.properties")
class BtcController {

    private val log = KLogging().logger
    @Value("\${btc.network}")
    private lateinit var network: BtcNetwork

    @GetMapping("/create/wallet")
    fun createWallet(): ResponseEntity<BtcWallet> {
        try {
            return ResponseEntity.ok<BtcWallet>(createWallet(network.params))
        } catch (e: Exception) {
            log.error("Error creating Bitcoin wallet", e)
            val response = BtcWallet(network = network)
            response.errorCode = e.javaClass.simpleName
            response.message = e.message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }

    private fun createWallet(netParams: NetworkParameters): BtcWallet {
        val wallet = Wallet(netParams)

        val out = ByteArrayOutputStream(1024)
        wallet.saveToFileStream(out)
        val walletBytes = out.toByteArray()
        out.reset()
        return BtcWallet(DatatypeConverter.printBase64Binary(walletBytes), network)
    }
}
