package jp.co.soramitsu.bootstrap.controller

import jp.co.soramitsu.bootstrap.dto.BtcNetwork
import jp.co.soramitsu.bootstrap.dto.BtcWallet
import mu.KLogging
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayOutputStream
import javax.xml.bind.DatatypeConverter
import kotlin.random.Random

@RestController
@RequestMapping("/btc")
class BtcController {

    private val log = KLogging().logger
    @Value("\${btc.network}")
    private lateinit var network:BtcNetwork

    @GetMapping("/create/wallet")
    fun createWallet(): ResponseEntity<BtcWallet> {
        try {
            return ResponseEntity.ok<BtcWallet>(createWallet(network.params))
        } catch (e: Exception) {
            log.error("Error creating Bitcoin wallet", e)
            val response = BtcWallet(network = network)
            response.errorCode = e.javaClass.simpleName
            response.message = e.message
            return ResponseEntity.ok<BtcWallet>(response)
        }
    }

    private fun createWallet(netParams: NetworkParameters): BtcWallet {
        val seedBytes = Random.nextBytes(DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS / 8)
        val mnemonic = MnemonicCode.INSTANCE.toMnemonic(seedBytes)
        val passphrase = ""
        val creationTimeSeconds = System.currentTimeMillis() / 1000

        val seed = DeterministicSeed(mnemonic, null, passphrase, creationTimeSeconds)
        val wallet = Wallet.fromSeed(netParams, seed)

        val out = ByteArrayOutputStream(1024)
        wallet.saveToFileStream(out)
        val walletBytes = out.toByteArray()
        out.reset()
        return BtcWallet(DatatypeConverter.printBase64Binary(walletBytes), network)
    }
}
