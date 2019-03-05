package jp.co.soramitsu.bootstrap.controller

import jp.co.soramitsu.bootstrap.dto.EthWallet
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.web3j.crypto.Keys
import org.web3j.crypto.Wallet
import org.web3j.crypto.WalletFile

@RestController
@RequestMapping("/eth")
class EthereumController {
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
            return ResponseEntity.ok<EthWallet>(response)
        }
    }
}
