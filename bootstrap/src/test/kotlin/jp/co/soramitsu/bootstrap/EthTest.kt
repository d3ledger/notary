package jp.co.soramitsu.bootstrap

import com.fasterxml.jackson.databind.ObjectMapper
import jp.co.soramitsu.bootstrap.dto.*
import mu.KLogging
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class EthTest {

    private val log = KLogging().logger

    @Autowired
    lateinit var mvc: MockMvc

    private val mapper = ObjectMapper()

    @Test
    @Ignore
    fun testDeploySmartContract() {
        val result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.post("/eth/deploy/D3/smartContracts").contentType(MediaType.APPLICATION_JSON).content(
                    mapper.writeValueAsString(
                        MasterContractsRequest(
                            network = EthereumNetworkProperties(
                                ethPasswords = EthereumPasswordsImpl(credentialsPassword = "password is specific for network creds"),
                                ethereumConfig = EthereumConfigImpl()),
                                notaryEthereumAccounts = listOf()
                        )
                    )
                )
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, MasterContractResponse::class.java)

        assertNull(respBody.errorCode)
    }

    @Test
    fun testCreateWallet() {
        val result: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/eth/create/wallet?password=abc"))
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
            .perform(MockMvcRequestBuilders.get("/eth/list/servicesWithWallet/d3/$peersCount"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val respBody = mapper.readValue(result.response.contentAsString, List::class.java)
        assertEquals(peersCount + 3, respBody.size)
    }
}
