package jp.co.soramitsu.bootstrap.controller

import jp.co.soramitsu.bootstrap.dto.EthWallet
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

    @GetMapping("/create/wallet")
    fun createWallet(@NotNull @RequestParam password:String): ResponseEntity<EthWallet> {
        try {
            val wallet: WalletFile = Wallet.createStandard(password, Keys.createEcKeyPair())
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
    fun listServiceEthWallets(@PathVariable("peersCount")peersCount: Int): ResponseEntity<List<String>> {
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
