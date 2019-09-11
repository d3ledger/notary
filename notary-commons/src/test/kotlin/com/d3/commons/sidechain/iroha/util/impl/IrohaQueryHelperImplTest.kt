/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.util.impl

import com.d3.commons.sidechain.iroha.util.IrohaPaginationHelper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import iroha.protocol.BlockOuterClass
import iroha.protocol.Primitive
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.QueryAPI
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import jp.co.soramitsu.iroha.testcontainers.PeerConfig
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IrohaQueryHelperImplTest {

    private val pageSize = 2
    private val crypto = Ed25519Sha3()
    private val peerKeypair = crypto.generateKeypair()
    private val rolename = "princess"
    private val domain = "duloc"
    private val accountName = "fiona"
    private val accountKeypair = crypto.generateKeypair()
    private val accountId = "$accountName@$domain"
    private val details: Map<String, String> by lazy {
        val map = HashMap<String, String>()
        repeat(10) {
            map["key$it"] = it.toString()
        }
        map
    }
    private val assets = mapOf("asset1" to "1", "asset2" to "2", "asset3" to "3")

    private val iroha = IrohaContainer().withPeerConfig(getPeerConfig())

    init {
        iroha.start()
    }

    private lateinit var queryAPI: QueryAPI
    private lateinit var queryHelper: IrohaQueryHelperImpl

    @BeforeEach
    fun initQueryHelper() {
        queryAPI = spy(QueryAPI(iroha.api, accountId, accountKeypair))
        queryHelper =
            IrohaQueryHelperImpl(queryAPI, IrohaPaginationHelper(queryAPI = queryAPI, pageSize = pageSize))
    }

    private fun getGenesisBlock(): BlockOuterClass.Block {
        val blockBuilder = GenesisBlockBuilder()
            .addTransaction(
                Transaction.builder(null)
                    .addPeer("0.0.0.0:10001", peerKeypair.public)
                    .createRole(
                        rolename,
                        listOf(
                            Primitive.RolePermission.can_get_all_acc_detail,
                            Primitive.RolePermission.can_set_detail,
                            Primitive.RolePermission.can_get_all_acc_ast,
                            Primitive.RolePermission.can_get_peers
                        )
                    ).createDomain(domain, rolename)
                    .createAccount(accountName, domain, accountKeypair.public)
                    .build() // returns ipj model Transaction
                    .build() // returns unsigned protobuf Transaction
            )

        val tx = Transaction.builder(accountId)
        details.forEach { (key, value) ->
            tx.setAccountDetail(accountId, key, value)
        }
        assets.forEach { (assetName, amount) ->
            tx.createAsset(assetName, domain, 1)
                .addAssetQuantity("$assetName#$domain", amount)
        }

        return blockBuilder.addTransaction(
            tx.build().build()
        ).build()
    }

    private fun getPeerConfig(): PeerConfig {
        val config = PeerConfig.builder()
            .genesisBlock(getGenesisBlock())
            .build()

        // don't forget to add peer keypair to config
        config.withPeerKeyPair(peerKeypair)

        return config
    }

    @AfterAll
    fun dropDown() {
        iroha.stop()
    }

    /**
     * @given queryHelper and Iroha populated with details
     * @when query account for all details
     * @then all account details are returned
     */
    @Test
    fun getAccountDetailsTest() {
        val actual = queryHelper.getAccountDetails(accountId, accountId).get()
        assertEquals(details, actual)
    }

    /**
     * @given queryHelper and Iroha populated with details
     * @when query details for an account that doesn't exist
     * @then empty map is returned
     */
    @Test
    fun getAccountDetailsEmptyAccountTest() {
        val actual = queryHelper.getAccountDetails(accountId, "empty@account").get()
        assertTrue(actual.isEmpty())
    }

    /**
     * @given queryHelper and Iroha populated with details
     * @when query account detail for "key1"
     * @then "1" is returned
     */
    @Test
    fun getAccountDetailsByKeyTest() {
        val actual = queryHelper.getAccountDetails(accountId, accountId, "key1").get()
        assertTrue(actual.isPresent)
        assertEquals(details["key1"], actual.get())
    }

    /**
     * @given queryHelper and Iroha populated with details
     * @when query account for key that doesn't exist
     * @then optional null is returned
     */
    @Test
    fun getNonExistAccountDetailsTest() {
        val actual = queryHelper.getAccountDetails(accountId, accountId, "nonexist_key").get()
        assertTrue(!actual.isPresent)
    }

    /**
     * @given queryHelper and Iroha populated with details
     * @when getAccountDetailsCount() is called with a predicate that returns false on every call
     * @then getAccountDetailsCount() returns zero
     */
    @Test
    fun getAccountDetailsCountUnsatisfiablePredicate() {
        assertEquals(0, queryHelper.getAccountDetailsCount(accountId, accountId) { _, _ -> false }.get())
        verify(queryAPI, times(calculatePaginationCalls(details.size, pageSize))).getAccountDetails(
            any(), any(), any(),
            any(), any(), any()
        )
    }

    /**
     * @given queryHelper and Iroha populated with details
     * @when getAccountDetailsCount() is called with a predicate that returns true on every call
     * @then getAccountDetailsCount() returns the number of all details in account
     */
    @Test
    fun getAccountDetailsCountTautologyPredicate() {
        assertEquals(details.size, queryHelper.getAccountDetailsCount(accountId, accountId) { _, _ -> true }.get())
        verify(queryAPI, times(calculatePaginationCalls(details.size, pageSize))).getAccountDetails(
            any(), any(), any(),
            any(), any(), any()
        )
    }

    /**
     * @given queryHelper and Iroha populated with details
     * @when getAccountDetailsCount() is called with a predicate that returns true on every value >=5
     * @then getAccountDetailsCount() returns the number of details with value >=5
     */
    @Test
    fun getAccountDetailsCount() {
        val predicate = { _: String, value: String -> value.toInt() >= 5 }
        assertEquals(
            details.entries.filter { entry -> predicate(entry.key, entry.value) }.size,
            queryHelper.getAccountDetailsCount(accountId, accountId, predicate).get()
        )
        verify(queryAPI, times(calculatePaginationCalls(details.size, pageSize))).getAccountDetails(
            any(), any(), any(),
            any(), any(), any()
        )
    }

    /**
     * @given queryHelper and Iroha populated with details
     * @when getAccountDetailsFilter() is called with a predicate that returns false on every call
     * @then getAccountDetailsFilter() returns an empty map
     */
    @Test
    fun getAccountDetailsFilteredUnsatisfiablePredicate() {
        assertTrue(
            queryHelper.getAccountDetailsFilter(accountId, accountId) { _, _ -> false }.get().isEmpty()
        )
        verify(queryAPI, times(calculatePaginationCalls(details.size, pageSize))).getAccountDetails(
            any(), any(), any(),
            any(), any(), any()
        )
    }

    /**
     * @given queryHelper and Iroha populated with details
     * @when getAccountDetailsFilter() is called with a predicate that returns true on every call
     * @then getAccountDetailsFilter() returns a copy of all the details in account
     */
    @Test
    fun getAccountDetailsFilteredTautologyPredicate() {
        assertEquals(
            details.size,
            queryHelper.getAccountDetailsFilter(accountId, accountId) { _, _ -> true }.get().size
        )
        verify(queryAPI, times(calculatePaginationCalls(details.size, pageSize))).getAccountDetails(
            any(), any(), any(),
            any(), any(), any()
        )
    }

    /**
     * @given queryHelper and Iroha populated with details
     * @when getAccountDetailsFilter() is called with a predicate that returns true on every value >=5
     * @then getAccountDetailsFilter() returns details with a value >=5
     */
    @Test
    fun getAccountDetailsFiltered() {
        val predicate = { _: String, value: String -> value.toInt() >= 5 }
        assertEquals(
            details.entries.filter { entry -> predicate(entry.key, entry.value) }.map { it.value }.sorted(),
            queryHelper.getAccountDetailsFilter(
                accountId,
                accountId,
                predicate
            ).get().entries.toList().map { it.value }.sorted()
        )
        verify(queryAPI, times(calculatePaginationCalls(details.size, pageSize))).getAccountDetails(
            any(), any(), any(),
            any(), any(), any()
        )
    }

    /**
     * @given queryHelper and Iroha populated with details
     * @when getAccountDetailsFirst() is called with a predicate that returns false on every call
     * @then getAccountDetailsFirst() returns null
     */
    @Test
    fun getAccountDetailsFirstUnsatisfiablePredicate() {
        assertFalse(
            queryHelper.getAccountDetailsFirst(
                accountId,
                accountId
            ) { _, _ -> false }.get().isPresent
        )
        verify(queryAPI, times(calculatePaginationCalls(details.size, pageSize))).getAccountDetails(
            any(), any(), any(),
            any(), any(), any()
        )
    }

    /**
     * @given queryHelper and Iroha populated with details
     * @when getAccountDetailsFirst() is called with a predicate that returns true on every call
     * @then getAccountDetailsFirst() returns some value. Paginated query call is called just once.
     */
    @Test
    fun getAccountDetailsFirstTautologyPredicate() {
        assertTrue(
            queryHelper.getAccountDetailsFirst(
                accountId,
                accountId
            ) { _, _ -> true }.get().isPresent
        )
        verify(queryAPI).getAccountDetails(
            any(), any(), any(),
            any(), any(), any()
        )
    }

    /**
     * @given queryHelper and Iroha populated with details
     * @when getAccountDetailsFirst() is called with a predicate that returns true on every value that equals to 5
     * @then getAccountDetailsFirst() returns a detail with value that equals to 5
     */
    @Test
    fun getAccountDetailsFirst() {
        val predicate = { _: String, value: String -> value.toInt() == 5 }
        assertEquals(
            details.entries.first { entry -> predicate(entry.key, entry.value) }.key,
            queryHelper.getAccountDetailsFirst(
                accountId,
                accountId,
                predicate
            ).get().get().key
        )
    }

    /**
     * @given queryHelper and one Iroha peer
     * @when getPeersCount() is called
     * @then '1' is returned
     */
    @Test
    fun getPeersCountTest() {
        val peers = queryHelper.getPeersCount().get()
        assertEquals(1, peers)
    }

    /**
     * @given queryHelper and Iroha populated with assets
     * @when query account asset "asset1"
     * @then returns amount "1" of "asset1"
     */
    @Test
    fun getAccountAssetTest() {
        val res = queryHelper.getAccountAsset(accountId, "asset1#$domain").get()
        assertEquals("1", res)
    }

    /**
     * @given queryHelper and Iroha populated with assets
     * @when query account assets
     * @then list of assets belonging to account is returned
     */
    @Test
    fun getAccountAssetsTest() {
        val actual = queryHelper.getAccountAssets(accountId).get()
        assertEquals(assets.mapKeys { (assetName, _) -> "$assetName#$domain" }, actual)
    }

    /**
     * @given queryHelper
     * @when query balance of "nonexist#domain" asset
     * @then return "0"
     */
    @Test
    fun getAccountNonExistAsset() {
        val res = queryHelper.getAccountAsset(accountId, "nonexist#$domain").get()
        assertEquals("0", res)
    }

    /**
     * Calculates the number of pagination calls
     * @param detailsSize - how many details in account
     * @param pageSize - size of page
     * @return number of calls
     */
    private fun calculatePaginationCalls(detailsSize: Int, pageSize: Int): Int {
        return when {
            detailsSize < pageSize -> return 1
            detailsSize % pageSize == 0 -> detailsSize / pageSize
            else -> detailsSize / pageSize + 1
        }
    }
}
