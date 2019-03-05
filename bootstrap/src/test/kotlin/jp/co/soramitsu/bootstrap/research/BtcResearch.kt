package jp.co.soramitsu.bootstrap.research

import com.fasterxml.jackson.databind.ObjectMapper
import jp.co.soramitsu.bootstrap.dto.BtcWallet
import mu.KLogging
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.HDUtils
import org.bitcoinj.wallet.DeterministicKeyChain
import org.bitcoinj.wallet.DeterministicSeed
import org.junit.Test
import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.params.RegTestParams
import org.json.JSONObject
import org.web3j.crypto.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import javax.xml.bind.DatatypeConverter
import kotlin.random.Random





class BtcResearch {

    private val log = KLogging().logger
    private val mapper = ObjectMapper()

    @Test
    fun testWithMnemonicGeneration() {
        val networkParams = RegTestParams.get()
        val seedBytes = Random.nextBytes(DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS / 8)
        val mnemonic = MnemonicCode.INSTANCE.toMnemonic(seedBytes)
        val passphrase = ""
        val creationTimeSeconds = System.currentTimeMillis() / 1000

        val ds2 = DeterministicSeed(mnemonic, null, passphrase, creationTimeSeconds)

        val wallet = org.bitcoinj.wallet.Wallet.fromSeed(networkParams, ds2)
        log.info(wallet.toString())
       // wallet.saveToFile(File("."))
    }

    @Test
    fun generateBtcWalletFile() {
        val networkParams = RegTestParams.get()
        val seedBytes = Random.nextBytes(DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS / 8)
        val mnemonic = MnemonicCode.INSTANCE.toMnemonic(seedBytes)

        val seed =
            DeterministicSeed(seedBytes, mnemonic, MnemonicCode.BIP39_STANDARDISATION_TIME_SECS);
        val wallet = org.bitcoinj.wallet.Wallet.fromSeed(networkParams, seed)
        log.info(wallet.toString())

    }

    @Test
    fun generateEthCredentialsFromBytesWithBitcoinJ() {

        val seedBytes = Random.nextBytes(DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS / 8)
        val mnemonic = ArrayList<String>(0)

        val seed =
            DeterministicSeed(seedBytes, mnemonic, MnemonicCode.BIP39_STANDARDISATION_TIME_SECS);
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

    @Test
    fun generateCredentialsFromBytesWeb3jOnly() {
        val seed = UUID.randomUUID().toString()

        val processJson = JSONObject()

        try {
            val ecKeyPair = Keys.createEcKeyPair()
            val privateKeyInDec = ecKeyPair.getPrivateKey()

            val sPrivatekeyInHex = privateKeyInDec.toString(16)

            val aWallet = Wallet.createLight(seed, ecKeyPair);
            val sAddress = aWallet.getAddress();


            processJson.put("address", "0x" + sAddress);
            processJson.put("privatekey", sPrivatekeyInHex);

            log.info(processJson.toString())
        } catch (e: Exception) {
            log.error("Exception happened", e)
        }
    }

    @Test
    fun generateKeyStoreFile1() {
        try {
            val password = "secr3t"
            val keyPair = Keys.createEcKeyPair()
            val wallet = Wallet.createStandard(password, keyPair)

            println("Priate key: " + keyPair.privateKey.toString(16))
            System.out.println("Account: " + wallet.address)

        } catch (e: Exception) {
            System.err.println("Error: " + e.message)
        }

    }
}
