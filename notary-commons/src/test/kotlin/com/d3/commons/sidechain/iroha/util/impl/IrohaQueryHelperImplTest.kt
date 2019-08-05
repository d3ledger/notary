/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.iroha.util.impl

import iroha.protocol.BlockOuterClass
import iroha.protocol.Primitive
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.QueryAPI
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import jp.co.soramitsu.iroha.testcontainers.PeerConfig
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IrohaQueryHelperImplTest {

    private val crypto = Ed25519Sha3()
    private val peerKeypair = crypto.generateKeypair()
    private val rolename = "princess"
    private val domain = "duloc"
    private val accountName = "fiona"
    private val accountKeypair = crypto.generateKeypair()
    private val accountId = "$accountName@$domain"
    private val details = mapOf("key1" to "value1", "key2" to "value2", "key3" to "value3")
    private val assets = mapOf("asset1" to "1", "asset2" to "2", "asset3" to "3")

    private val iroha = IrohaContainer().withPeerConfig(getPeerConfig())

    init {
        iroha.start()
    }

    val queryHelper =
        IrohaQueryHelperImpl(QueryAPI(iroha.api, accountId, accountKeypair), pageSize = 2)

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

    fun getPeerConfig(): PeerConfig {
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
     * @then "value1" is returned
     */
    @Test
    fun getAccountDetailTest() {
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

}
