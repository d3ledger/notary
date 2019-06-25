/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.iroha

import com.d3.commons.expansion.ExpansionDetails
import com.d3.commons.expansion.ExpansionUtils
import com.d3.commons.expansion.ServiceExpansion
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.IrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.util.toHexString
import com.github.kittinunf.result.failure
import integration.helper.IrohaIntegrationHelperUtil
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServiceExpansionTest {

    private val peers = 3
    private val integrationHelperUtil = IrohaIntegrationHelperUtil(peers)
    private val irohaQueryHelper = IrohaQueryHelperImpl(
        integrationHelperUtil.irohaAPI,
        integrationHelperUtil.accountHelper.notaryAccount.accountId,
        integrationHelperUtil.accountHelper.notaryAccount.keyPair
    )

    /**
     * Expansion logic that adds signatory to account
     */
    private fun expansionLogic(
        mstAccount: IrohaCredential,
        expansionDetails: ExpansionDetails,
        triggerTime: Long
    ) {
        ExpansionUtils.addSignatureExpansionLogic(
            integrationHelperUtil.irohaAPI,
            mstAccount,
            expansionDetails,
            triggerTime
        ).failure { ex -> throw ex }
    }

    /**
     * Creates listener that listens to expansion events
     */
    private fun createExpansionListener(accountToExpand: IrohaCredential) {
        IrohaChainListener(
            integrationHelperUtil.irohaAPI,
            integrationHelperUtil.accountHelper.notaryAccount
        ).getBlockObservable().get().subscribe { block ->
            ServiceExpansion(
                integrationHelperUtil.accountHelper.expansionTriggerAccount.accountId,
                integrationHelperUtil.accountHelper.expansionCreatorAccount,
                integrationHelperUtil.irohaAPI
            ).expand(block) { details, hash, time ->
                expansionLogic(
                    accountToExpand,
                    details,
                    time
                )
            }
        }
    }

    @BeforeAll
    fun setUp() {
        // Make notary account MST
        integrationHelperUtil.accountHelper.makeAccountMst(
            integrationHelperUtil.accountHelper.notaryAccount
        ).forEach {
            // Run expansion listeners
            createExpansionListener(it)
        }
    }

    @AfterAll
    fun tearDown() {
        integrationHelperUtil.close()
    }

    /**
     * Triggers expansion process
     * @param accountId - account to expand
     * @param publicKey - new public key
     * @param quorum - new quorum
     */
    private fun triggerExpansion(
        accountId: String,
        publicKey: String,
        quorum: Int
    ) {
        val expansionDetails = ExpansionDetails(
            accountId,
            publicKey,
            quorum
        )
        IrohaConsumerImpl(
            integrationHelperUtil.accountHelper.superuserAccount,
            integrationHelperUtil.irohaAPI
        ).send(
            ExpansionUtils.createExpansionTriggerTx(
                integrationHelperUtil.accountHelper.superuserAccount.accountId,
                expansionDetails,
                integrationHelperUtil.accountHelper.expansionTriggerAccount.accountId
            )
        ).failure { ex -> throw ex }
    }

    /**
     * @given 3 expansion services being started
     * @when expansion service is triggered
     * @then notary account quorum becomes 4
     */
    @Test
    fun testUpdateNotaryQuorum() {
        // Random pubkey
        val publicKey = Ed25519Sha3().generateKeypair().public.toHexString()
        // Trigger expansion
        triggerExpansion(
            integrationHelperUtil.accountHelper.notaryAccount.accountId,
            publicKey,
            peers + 1
        )
        // Wait a little
        Thread.sleep(5_000)
        // Get new notary quorum
        val notaryQuorum =
            irohaQueryHelper.getAccountQuorum(integrationHelperUtil.accountHelper.notaryAccount.accountId)
        // Check that it was increased
        assertEquals(peers + 1, notaryQuorum.get())
    }
}
