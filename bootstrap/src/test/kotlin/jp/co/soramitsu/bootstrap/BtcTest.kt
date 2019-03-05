package jp.co.soramitsu.bootstrap

import com.fasterxml.jackson.databind.ObjectMapper
import jp.co.soramitsu.bootstrap.dto.BtcNetwork
import jp.co.soramitsu.bootstrap.dto.BtcWallet
import jp.co.soramitsu.bootstrap.dto.EthWallet
import mu.KLogging
import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.Wallet
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testcontainers.shaded.org.apache.commons.io.FileUtils
import java.io.ByteArrayOutputStream
import java.io.File
import javax.xml.bind.DatatypeConverter
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class BtcTest {

    @Autowired
    lateinit var mvc: MockMvc

    private val mapper = ObjectMapper()

    private val log = KLogging().logger


    @Test
    fun bitcoinCreateWalletTest() {
        var result: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/btc/create/wallet"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        var respBody = mapper.readValue(result.response.contentAsString, BtcWallet::class.java)
        assertNull(respBody.errorCode)
        assertNull(respBody.message)
        assertNotNull(respBody.file)

        result = mvc
            .perform(MockMvcRequestBuilders.get("/btc/create/wallet"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        respBody = mapper.readValue(result.response.contentAsString, BtcWallet::class.java)
        assertNull(respBody.errorCode)
        assertNull(respBody.message)
        assertNotNull(respBody.file)
        assertEquals(BtcNetwork.RegTest, respBody.network)
       /* val binary = DatatypeConverter.parseBase64Binary(respBody.file)
        FileUtils.writeByteArrayToFile(File("btc-wallet-test.wallet"), binary)*/
    }

    @Test
    fun walletWithouSeed() {
        val seedBytes = Random.nextBytes(DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS / 8)
        val mnemonic = MnemonicCode.INSTANCE.toMnemonic(seedBytes)
        val passphrase = ""
        val creationTimeSeconds = System.currentTimeMillis() / 1000

        val seed = DeterministicSeed(mnemonic, null, passphrase, creationTimeSeconds)
        val wallet = Wallet(RegTestParams.get())
        log.info("some info")
    }
}
