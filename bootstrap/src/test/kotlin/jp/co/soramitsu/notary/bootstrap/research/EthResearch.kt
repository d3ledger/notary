package jp.co.soramitsu.notary.bootstrap.research

import mu.KLogging
import org.bitcoinj.crypto.HDUtils
import org.bitcoinj.wallet.DeterministicKeyChain
import org.bitcoinj.wallet.DeterministicSeed
import org.junit.Test
import org.web3j.crypto.Credentials

class EthResearch {

    private val log = KLogging().logger

    @Test
    fun generateCredentials() {

        val seedCode = "yard impulse luxury trive today throw farm pepper glady survey wreck glass federal"
//Example of credentials generation taken from here https://stackoverflow.com/questions/49201637/how-to-generate-deterministic-keys-for-ethereum-using-java
// BitcoinJ

        val seed = DeterministicSeed(seedCode, null, "", 1409478661L)
        val chain = DeterministicKeyChain.builder().seed(seed).build()
        val keyPath = HDUtils.parsePath("M/44H/60H/0H/0/0")
        val key = chain.getKeyByPath(keyPath, true)
        val privKey = key.getPrivKey()

// Web3j
        val credentials = Credentials.create(privKey.toString(16))
        log.info("Address: ${credentials.address}")
        log.info("Private: ${credentials.ecKeyPair.privateKey.toString(16)}")
        log.info("Public: ${credentials.ecKeyPair.publicKey.toString(16)}")

    }
}