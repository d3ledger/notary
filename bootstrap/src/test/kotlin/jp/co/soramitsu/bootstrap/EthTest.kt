/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

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
import java.io.File
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

    val classLoader = javaClass.classLoader

    @Test
    @Ignore
    fun testUpdateSmartContractAddPeer() {
        val file = File(classLoader.getResource("eth/main-net-genesis.key")!!.file)

        val result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.post("/eth/deploy/D3/masterContract/update").contentType(
                    MediaType.APPLICATION_JSON
                ).content(
                    mapper.writeValueAsString(
                        UpdateMasterContractRequest(
                            network = EthereumNetworkProperties(
                                ethPasswords = EthereumPasswordsImpl(
                                    credentialsPassword = "joms...",
                                    nodeLogin = "devel..",
                                    nodePassword = "emooy..."
                                ),
                                ethereumConfig = EthereumConfigImpl(
                                    url = "https://parity1...",
                                    credentialsPath = file.absolutePath,
                                    gasPrice = 10000000000,
                                    gasLimit = 4500000,
                                    confirmationPeriod = 20
                                )
                            ),
                            masterContract = MasterContractProperties(
                                address = "0x7d1a4fd3d286e5eb239f7f081481ab5be3517c00"
                            ),
                            newPeerAddress = "0x7432fc601d81362f9492ded6d4a670bcd25970d0"
                        )
                    )
                )
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(
            result.response.contentAsString,
            UpdateMasterContractResponse::class.java
        )

        assertNull(respBody.errorCode)
        assertNotNull(respBody.success)
    }

    @Test
    @Ignore
    fun testDeployAllInitialContractsByDifferentEndpointsMainNet() {
        val file = File(classLoader.getResource("eth/main-net-genesis.key")!!.file)
        val network = EthereumNetworkProperties(
            ethPasswords = EthereumPasswordsImpl(
                credentialsPassword = "joms...",
                nodeLogin = "devel...",
                nodePassword = "emooy..."
            ),
            ethereumConfig = EthereumConfigImpl(
                url = "https://parity1....",
                credentialsPath = file.absolutePath,
                gasPrice = 10000000000,
                gasLimit = 4500000,
                confirmationPeriod = 20
            )
        )

        Thread.sleep(10000)

        var result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.post("/eth/deploy/D3/relayRegistry").contentType(MediaType.APPLICATION_JSON).content(
                    mapper.writeValueAsString(
                        network
                    )
                )
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val relayRegistryResp =
            mapper.readValue(
                result.response.contentAsString,
                DeploySmartContractResponse::class.java
            )
        log.info("Deploy RelayRegistry Response: ${result.response.contentAsString}")

        Thread.sleep(10000)

        result = mvc
            .perform(
                MockMvcRequestBuilders.post("/eth/deploy/D3/masterContract").contentType(MediaType.APPLICATION_JSON).content(
                    mapper.writeValueAsString(
                        DeployMasterContractRequest(
                            network = network,
                            notaryEthereumAccounts = listOf("0x7432fc601d81362f9924ded6d4a670bcd25970d0"),
                            relayRegistryAddress = relayRegistryResp.contractAddress
                                ?: throw RuntimeException("relayRegistry response is without contract address")
                        )
                    )
                )
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val masterContractResp =
            mapper.readValue(
                result.response.contentAsString,
                DeployMasterContractResponse::class.java
            )
        log.info("Deploy MasterContract Response: ${result.response.contentAsString}")

        Thread.sleep(10000)

        result = mvc
            .perform(
                MockMvcRequestBuilders.post("/eth/deploy/D3/relayImplementation").contentType(
                    MediaType.APPLICATION_JSON
                ).content(
                    mapper.writeValueAsString(
                        DeployRelayImplementationRequest(
                            network = network,
                            masterContractAddress = masterContractResp.contractAddress
                                ?: throw RuntimeException("relayRegistry response is without contract address")

                        )
                    )
                )
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val relayImplementationResp =
            mapper.readValue(
                result.response.contentAsString,
                DeploySmartContractResponse::class.java
            )
        log.info("Deploy RelayImplementation Response: ${result.response.contentAsString}")
    }

    @Test
    @Ignore
    fun testDeploySmartContractMainNet() {
        val file = File(classLoader.getResource("eth/main-net-genesis.key")!!.file)

        val result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.post("/eth/deploy/D3/smartContracts").contentType(MediaType.APPLICATION_JSON).content(
                    mapper.writeValueAsString(
                        AllInitialContractsRequest(
                            network = EthereumNetworkProperties(
                                ethPasswords = EthereumPasswordsImpl(
                                    credentialsPassword = "joms...",
                                    nodeLogin = "devel...",
                                    nodePassword = "emooy..."
                                ),
                                ethereumConfig = EthereumConfigImpl(
                                    url = "https://parity1....",
                                    credentialsPath = file.absolutePath,
                                    gasPrice = 10000000000,
                                    gasLimit = 4500000,
                                    confirmationPeriod = 20
                                )
                            ),
                            notaryEthereumAccounts = listOf("0x7432fc601d81362f9924ded6d4a670bcd25970d0")
                        )
                    )
                )
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(
            result.response.contentAsString,
            DeployInitialContractsResponse::class.java
        )
        log.info("DeployResponse: ${result.response.contentAsString}")
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
        assertEquals(peersCount + 4, respBody.size)
    }
}
