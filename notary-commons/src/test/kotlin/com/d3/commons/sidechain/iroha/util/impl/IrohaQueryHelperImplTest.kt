/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.util.impl

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.isNull
import com.nhaarman.mockito_kotlin.mock
import iroha.protocol.QryResponses
import jp.co.soramitsu.iroha.java.QueryAPI
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class IrohaQueryHelperImplTest {

    /**
     * @given queryHelper
     * @when query balance of "nonexist#domain" asset
     * @then return "0"
     */
    @Test
    fun getNonexistentAccountAssetTest() {
        val clientId = "client@domain"
        val assetId = "nonexist#domain"

        val emptyResponse = QryResponses.AccountAssetResponse.newBuilder().build()
        val queryAPI = mock<QueryAPI>() {
            on { getAccountAssets(eq(clientId)) } doReturn emptyResponse
        }

        val queryHelper = IrohaQueryHelperImpl(queryAPI)

        val res = queryHelper.getAccountAsset(clientId, assetId)
        assertEquals("0", res.get())
    }

    /**
     * @given queryHelper
     * @when query details of "writer#domain" that does not exist
     * @then return null
     */
    @Test
    fun getEmptyAccountData() {
        val storageClientId = "client@domain"
        val writerClientId = "writer@domain"

        val emptyResponse = "{\"writer@domain\" : null}"
        val queryAPI = mock<QueryAPI>() {
            on {
                getAccountDetails(
                    eq(storageClientId),
                    eq(writerClientId),
                    isNull()
                )
            } doReturn emptyResponse
        }

        val queryHelper = IrohaQueryHelperImpl(queryAPI)

        val res = queryHelper.getAccountDetails(storageClientId, writerClientId)
        assertEquals(emptyMap(), res.get())
    }

    /**
     * @given queryHelper
     * @when query details of "account@a_domain"
     * @then return details
     */
    @Test
    fun getAccountData() {
        val storageClientId = "client@domain"
        val writerA = "account@a_domain"

        val emptyResponse = "{\n" +
                "    \"account@a_domain\": {\n" +
                "        \"age\": 18,\n" +
                "        \"hobbies\": \"crypto\"\n" +
                "    }\n" +
                "}"
        val queryAPI = mock<QueryAPI>() {
            on {
                getAccountDetails(
                    eq(storageClientId),
                    eq(writerA),
                    isNull()
                )
            } doReturn emptyResponse
        }

        val queryHelper = IrohaQueryHelperImpl(queryAPI)

        val resA = queryHelper.getAccountDetails(storageClientId, writerA)
        assertEquals(mapOf("age" to "18", "hobbies" to "crypto"), resA.get())
    }
}
