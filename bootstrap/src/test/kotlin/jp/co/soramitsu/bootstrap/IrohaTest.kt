package jp.co.soramitsu.bootstrap

import com.fasterxml.jackson.databind.ObjectMapper
import jp.co.soramitsu.bootstrap.dto.*
import jp.co.soramitsu.bootstrap.dto.block.GenesisBlock
import jp.co.soramitsu.bootstrap.exceptions.ErrorCodes
import jp.co.soramitsu.bootstrap.genesis.d3.D3TestGenesisFactory
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
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
import java.util.stream.Collectors.toList
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
        assertEquals(
            "{\"errorCode\":null,\"message\":null,\"projects\":[{\"project\":\"D3\",\"environments\":[\"test\"]}]}",
            respBody
        )
    }

    @Test
    fun testAccountsNeeded() {
        val result: MvcResult = mvc
            .perform(get("/iroha/config/accounts/D3/test/5"))
            .andExpect(status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, NeededAccountsResponse().javaClass)
        val activeAccounts = ArrayList<AccountPrototype>()
        d3Genesis.getAccountsForConfiguration(5).forEach { if (!it.passive) activeAccounts.add(it) }
        assertEquals(activeAccounts.size, respBody.accounts.size)
        val dependentAccounts = respBody.accounts.stream().filter { it.peersDependentQuorum == true }.collect(toList())
        dependentAccounts.forEach { assertEquals(3, it.quorum) }
    }

    @Test
    fun testZeroPeersAccountsNeeded() {
        val result: MvcResult = mvc
            .perform(get("/iroha/config/accounts/D3/test/0"))
            .andExpect(status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, NeededAccountsResponse().javaClass)
        assertEquals(respBody.errorCode, ErrorCodes.INCORRECT_PEERS_COUNT.name)
    }

    @Test
    fun testOnePeerAccountsNeeded() {
        val result: MvcResult = mvc
            .perform(get("/iroha/config/accounts/D3/test/1"))
            .andExpect(status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, NeededAccountsResponse().javaClass)
        val dependentAccounts = respBody.accounts.stream().filter { it.peersDependentQuorum == true }.collect(toList())
        dependentAccounts.forEach { assertEquals(1, it.quorum) }
    }

    @Test
    fun testTwoPeersAccountsNeeded() {
        val result: MvcResult = mvc
            .perform(get("/iroha/config/accounts/D3/test/2"))
            .andExpect(status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, NeededAccountsResponse().javaClass)
        val dependentAccounts = respBody.accounts.stream().filter { it.peersDependentQuorum == true }.collect(toList())
        dependentAccounts.forEach { assertEquals(1, it.quorum) }
    }

    @Test
    fun testEmptyPeerKey() {
        val peerKey1 = generatePublicKeyHex()

        mvc
            .perform(
                post("/iroha/create/genesisBlock").contentType(MediaType.APPLICATION_JSON).content(
                    mapper.writeValueAsString(
                        jp.co.soramitsu.bootstrap.dto.GenesisRequest(
                            peers = listOf(
                                Peer(peerKey1, "firstTHost:12435"),
                                Peer("", "secondTHost:987654")
                            )
                        )
                    )
                )
            )
            .andExpect(status().is4xxClientError)
            .andReturn()
    }

    @Test
    fun testGenesisBlock() {
        val peerKey1 = generatePublicKeyHex()
        val peerKey2 = generatePublicKeyHex()
        val notaryAddress1 = "notaryHost1:43652"
        val notaryAddress2 = "notaryHost2:3652"
        val peers = listOf(
            Peer(peerKey1, "firstTHost:12435", notaryAddress1),
            Peer(peerKey2, "secondTHost:987654", notaryAddress2)
        )
        val accounts = getAccounts(peers.size)

        val result: MvcResult = mvc
            .perform(
                post("/iroha/create/genesisBlock").contentType(MediaType.APPLICATION_JSON).content(
                    mapper.writeValueAsString(
                        jp.co.soramitsu.bootstrap.dto.GenesisRequest(
                            peers = peers,
                            accounts = accounts
                        )
                    )
                )
            )
            .andExpect(status().isOk)
            .andReturn()

        val respBody = result.response.contentAsString
        log.info("Response: $respBody")

        //check peers added
        assertTrue(respBody.contains("firstTHost:12435"))
        assertTrue(respBody.contains("secondTHost:987654"))

        //Check passive - keyless accounts added
        assertTrue(respBody.contains("notaries"))
        assertTrue(respBody.contains("gen_btc_pk_trigge"))
        assertTrue(respBody.contains("btc_change_addresses"))

        assertTrue(respBody.contains(peerKey1))
        assertTrue(respBody.contains(peerKey2))
        assertTrue(respBody.contains(notaryAddress1))
        assertTrue(respBody.contains(notaryAddress2))

        val quorumCheck =
            "{\\\"accountId\\\":\\\"mst_btc_registration_service@notary\\\",\\\"quorum\\\":${peers.size - peers.size / 3}}"
        assertTrue(respBody.contains(quorumCheck))
        accounts.forEach {
            val pubKey = it.pubKeys[0]
            assertTrue(
                respBody.contains(pubKey),
                "pubKey not exists in block when should for account ${it.accountName}@${it.domainId} pubKey:${it.pubKeys[0]}"
            )
        }

        val genesisResponse =
            mapper.readValue(respBody, GenesisResponse::class.java)
        assertNotNull(genesisResponse)
        val genesisBlock = mapper.readValue(
            genesisResponse.blockData,
            GenesisBlock::class.java
        )
        assertNotNull(genesisBlock)
    }

    private fun getAccounts(peersCount: Int): List<AccountPublicInfo> {
        return listOf(
            createAccountDto("notary", "notary", peersCount - peersCount / 3),
            createAccountDto("registration_service", "notary"),
            createAccountDto("eth_registration_service", "notary"),
            createAccountDto("btc_registration_service", "notary"),
            createAccountDto("mst_btc_registration_service", "notary", peersCount - peersCount / 3),
            createAccountDto("eth_token_storage_service", "notary"),
            createAccountDto("withdrawal", "notary"),
            createAccountDto("btc_fee_rate", "notary"),
            createAccountDto("btc_withdrawal_service", "notary", peersCount - peersCount / 3),
            createAccountDto("btc_sign_collector", "notary"),
            createAccountDto("btc_change_addresses", "notary"),
            createAccountDto("test", "notary"),
            createAccountDto("vacuumer", "notary"),
            createAccountDto("gen_btc_pk_trigger", "notary"),
            createAccountDto("admin", "notary"),
            createAccountDto("sora", "sora"),
            createAccountDto("brvs", "brvs"),
            createAccountDto("client_account", "notary")
        )
    }

    private fun createAccountDto(title: String, domain: String, quorum: Int = 1): AccountPublicInfo {
        return AccountPublicInfo(
            listOf(
                generatePublicKeyHex()
            ), domain, title, quorum
        )
    }

    private fun generatePublicKeyHex() =
        DatatypeConverter.printHexBinary(Ed25519Sha3().generateKeypair().public.encoded)
}

