package jp.co.soramitsu.bootstrap

import com.fasterxml.jackson.databind.ObjectMapper
import jp.co.soramitsu.bootstrap.dto.AccountPublicInfo
import jp.co.soramitsu.bootstrap.dto.Peer
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.bootstrap.genesis.d3.D3TestGenesisFactory
import junit.framework.TestCase.*
import mu.KLogging
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import javax.xml.bind.DatatypeConverter
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class IrohaTest {

    private val log = KLogging().logger

    @Autowired
    lateinit var mvc: MockMvc

    val d3Genesis = D3TestGenesisFactory()

    private val mapper = ObjectMapper()

    @Test
    @Throws(Exception::class)
    fun keyPairTest() {
        val result: MvcResult = mvc
            .perform(get("/iroha/create/keyPair"))
            .andExpect(status().isOk)
            .andReturn()
        val respBody = result.response.contentAsString
        log.info("Response body: $respBody")
        assertTrue(respBody.contains("private"))
        assertTrue(respBody.contains("public"))
    }

    @Test
    fun testProjectsGenesis() {
        val result: MvcResult = mvc
            .perform(get("/iroha/projects/genesis"))
            .andExpect(status().isOk)
            .andReturn()
        val respBody = result.response.contentAsString
        assertEquals("{\"D3\":\"test\"}", respBody)
    }

    @Test
    fun testAccountsNeeded() {
        val result: MvcResult = mvc
            .perform(get("/iroha/config/accounts/D3/test"))
            .andExpect(status().isOk)
            .andReturn()

        val respBody = result.response.contentAsString
        val activeAccounts = ArrayList<jp.co.soramitsu.bootstrap.dto.AccountPrototype>()
        d3Genesis.getAccountsNeeded().forEach { if (!it.passive) activeAccounts.add(it) }
        assertEquals(mapper.writeValueAsString(activeAccounts), respBody)
    }

    @Test
    fun testGenesisBlock() {
        val peerKey1 = generatePublicKeyBase64()
        val peerKey2 = generatePublicKeyBase64()
        val accounts = getAccounts()

        val result: MvcResult = mvc
            .perform(
                post("/iroha/create/genesisBlock").contentType(MediaType.APPLICATION_JSON).content(
                    mapper.writeValueAsString(
                        jp.co.soramitsu.bootstrap.dto.GenesisRequest(
                            peers = listOf(
                                Peer(peerKey1, "firstTHost:12435"),
                                Peer(peerKey2, "secondTHost:987654")
                            ),
                            accounts = accounts
                        )
                    )
                )
            )
            .andExpect(status().isOk)
            .andReturn()

        val respBody = result.response.contentAsString
        log.info("Response: $respBody")

        assertTrue(respBody.contains("firstTHost:12435"))
        assertTrue(respBody.contains("secondTHost:987654"))
        log.info("peerKey1:$peerKey1")
        log.info("peerKey2:$peerKey2")
        assertTrue(respBody.contains(peerKey1))
        assertTrue(respBody.contains(peerKey2))
        accounts.forEach {
            val pubKey = it.pubKeys[0]
            assertTrue(
                respBody.contains(pubKey),
                "pubKey not exists in block when should for account ${it.accountName}@${it.domainId} pubKey:${it.pubKeys[0]}"
            )
        }

        val genesisResponse =
            mapper.readValue(respBody, jp.co.soramitsu.bootstrap.dto.GenesisResponse::class.java)
        assertNotNull(genesisResponse)
        val genesisBlock = mapper.readValue(
            genesisResponse.blockData,
            jp.co.soramitsu.bootstrap.dto.block.GenesisBlock::class.java
        )
        assertNotNull(genesisBlock)
    }

    private fun getAccounts(): List<AccountPublicInfo> {
        return listOf(
            createAccountDto("notary", "notary"),
            createAccountDto("registration_service", "notary"),
            createAccountDto("eth_registration_service", "notary"),
            createAccountDto("btc_registration_service", "notary"),
            createAccountDto("mst_btc_registration_service", "notary"),
            createAccountDto("eth_token_storage_service", "notary"),
            createAccountDto("withdrawal", "notary"),
            createAccountDto("btc_fee_rate", "notary"),
            createAccountDto("btc_withdrawal_service", "notary"),
            createAccountDto("btc_sign_collector", "notary"),
            createAccountDto("btc_change_addresses", "notary"),
            createAccountDto("test", "notary"),
            createAccountDto("vacuumer", "notary"),
            createAccountDto("notaries", "notary"),
            createAccountDto("gen_btc_pk_trigger", "notary"),
            createAccountDto("admin", "notary"),
            createAccountDto("sora", "sora")
        )
    }

    private fun createAccountDto(title: String, domain: String): AccountPublicInfo {
        return AccountPublicInfo(
            listOf(
                generatePublicKeyBase64()
            ), domain, title
        )
    }

    private fun generatePublicKeyBase64() =
        DatatypeConverter.printBase64Binary(Ed25519Sha3().generateKeypair().public.encoded)
}

