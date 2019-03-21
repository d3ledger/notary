package jp.co.soramitsu.bootstrap

import com.fasterxml.jackson.databind.ObjectMapper
import jp.co.soramitsu.bootstrap.dto.EthWallet
import jp.co.soramitsu.bootstrap.genesis.d3.D3TestGenesisFactory
import mu.KLogging
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
import org.web3j.crypto.WalletFile
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class EthTest {

    private val log = KLogging().logger

    @Autowired
    lateinit var mvc: MockMvc

    private val mapper = ObjectMapper()

    @Test
    fun testCredentialsRequest() {
        val result: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/eth/create/wallet"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, EthWallet::class.java)
        assertNull(respBody.errorCode)
        assertNull(respBody.message)
        val wallet = respBody.file
        assertNotNull(wallet)
        assertNotNull(wallet.address)
        assertNotNull(wallet.id)
        assertNotNull(wallet.version)
        assertNotNull(wallet.crypto)
        assertNotNull(wallet.crypto.cipher)
        assertNotNull(wallet.crypto.cipherparams.iv)
        assertNotNull(wallet.crypto.kdf)
    }

    @Test
    fun testListEthereumServicesWithWallets() {
        val peersCount = 6
        val result: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/eth/list/servicesWithWallet/$peersCount"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val respBody = mapper.readValue(result.response.contentAsString, List::class.java)
        assertEquals(peersCount + 3, respBody.size)
    }
}
