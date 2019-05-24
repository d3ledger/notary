/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap

import com.fasterxml.jackson.databind.ObjectMapper
import jp.co.soramitsu.bootstrap.dto.BtcNetwork
import jp.co.soramitsu.bootstrap.dto.BtcWallet
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
        /* Uncomment if you'd like to look at file
          val binary = DatatypeConverter.parseBase64Binary(respBody.file)
         FileUtils.writeByteArrayToFile(File("btc-wallet-test.wallet"), binary)*/
    }
}
