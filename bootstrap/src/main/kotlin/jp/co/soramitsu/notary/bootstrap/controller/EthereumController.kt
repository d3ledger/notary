package jp.co.soramitsu.notary.bootstrap.controller

import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.bitcoinj.crypto.HDUtils
import org.bitcoinj.wallet.DeterministicKeyChain
import org.bitcoinj.wallet.DeterministicSeed
import org.web3j.crypto.Credentials


@RestController
@RequestMapping("/ethereum")
class EthereumController {

    private val log = KLogging().logger

  //  @GetMapping("/create/address")
    fun generateGenericBlock(): ResponseEntity<jp.co.soramitsu.notary.bootstrap.dto.BlockchainCreds> {
        log.info("Request of ethereun creds")

        val seedCode = "yard impulse luxury drive today throw farm pepper survey wreck glass federal"
//Example of credentials generation taken from here https://stackoverflow.com/questions/49201637/how-to-generate-deterministic-keys-for-ethereum-using-java
// BitcoinJ
        val seed = DeterministicSeed(seedCode, null, "", 1409478661L)
        val chain = DeterministicKeyChain.builder().seed(seed).build()
        val keyPath = HDUtils.parsePath("M/44H/60H/0H/0/0")
        val key = chain.getKeyByPath(keyPath, true)
        val privKey = key.getPrivKey()

// Web3j
        val credentials = Credentials.create(privKey.toString(16))
        return ResponseEntity.ok<jp.co.soramitsu.notary.bootstrap.dto.BlockchainCreds>(
            jp.co.soramitsu.notary.bootstrap.dto.BlockchainCreds(credentials.ecKeyPair.privateKey.toString(16),
                credentials.ecKeyPair.publicKey.toString(16))
        )
    }

}