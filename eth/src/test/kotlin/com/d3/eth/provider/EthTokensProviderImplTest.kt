/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.provider

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.github.kittinunf.result.Result
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EthTokensProviderImplTest {

    val ethAnchoredResult = Result.of {
        mapOf(
            "0x0001" to "token_1#ethereum",
            "0x0002" to "token_2#ethereum",
            "0x0003" to "token_3#ethereum"
        )
    }

    val irohaAnchoredResult = Result.of {
        mapOf(
            "0xAAA1" to "token_1#sora",
            "0xAAA2" to "token_2#sora",
            "0xAAA3" to "token_3#sora"
        )
    }

    val wrongAssetId = "wrong#asset"

    private val ethAnchoredTokenStorageAccount = "eth_anchored_token_storage@notary"
    private val ethAnchoredTokenSetterAccount = "eth_anchored_token_setter@notary"
    private val irohaAnchoredTokenStorageAccount = "iroha_anchored_token_storage@notary"
    private val irohaAnchoredTokenSetterAccount = "iroha_anchored_token_setter@notary"

    val irohaQueryHelper = mock<IrohaQueryHelper> {
        on {
            getAccountDetails(
                eq(ethAnchoredTokenStorageAccount),
                eq(ethAnchoredTokenSetterAccount)
            )
        } doReturn ethAnchoredResult

        on {
            getAccountDetails(
                eq(irohaAnchoredTokenStorageAccount),
                eq(irohaAnchoredTokenSetterAccount)
            )
        } doReturn irohaAnchoredResult
    }

    val ethTokenProvider = EthTokensProviderImpl(
        irohaQueryHelper,
        ethAnchoredTokenStorageAccount,
        ethAnchoredTokenSetterAccount,
        irohaAnchoredTokenStorageAccount,
        irohaAnchoredTokenSetterAccount
    )

    @Test
    fun getTokenAddressTest() {
        ethAnchoredResult.get()
            .forEach { (address, assetId) ->
                assertEquals(address, ethTokenProvider.getTokenAddress(assetId).get())
            }

        irohaAnchoredResult.get()
            .forEach { (address, assetId) ->
                assertEquals(address, ethTokenProvider.getTokenAddress(assetId).get())
            }
    }

    @Test
    fun getWrongAssetIdTest() {
        assertThrows<IllegalArgumentException> {
            ethTokenProvider.getTokenAddress(wrongAssetId).get()
        }
    }
}
