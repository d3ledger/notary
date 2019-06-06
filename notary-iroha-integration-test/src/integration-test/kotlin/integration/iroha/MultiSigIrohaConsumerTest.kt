/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.iroha

import com.d3.commons.sidechain.iroha.consumer.MultiSigIrohaConsumer
import com.d3.commons.sidechain.iroha.util.ModelUtil
import integration.helper.IrohaConfigHelper
import integration.helper.IrohaIntegrationHelperUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Duration

private const val PEERS = 3

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MultiSigIrohaConsumerTest {

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    private val integrationHelperUtil = IrohaIntegrationHelperUtil(PEERS)

    private val notaryConsumers = ArrayList<MultiSigIrohaConsumer>()

    @BeforeAll
    fun setUp() {
        notaryConsumers.addAll(
            // Make notary account MultiSig
            integrationHelperUtil.accountHelper.makeAccountMst(
                integrationHelperUtil.accountHelper.notaryAccount
            ).map { credential ->
                MultiSigIrohaConsumer(
                    credential,
                    integrationHelperUtil.irohaAPI
                )
            }
        )
    }

    /**
     * @given notary account with quorum set to 3
     * @when transaction is sent 3 times using MultisigIrohaConsumer
     * @then it's committed in Iroha
     */
    @Test
    fun testCommitted() {
        val testKey = "key_1"
        val testValue = "value_1"
        val creationTime = System.currentTimeMillis()
        notaryConsumers.forEach { consumer ->
            ModelUtil.setAccountDetail(
                irohaConsumer = consumer,
                accountId = consumer.creator,
                key = testKey,
                value = testValue,
                createdTime = creationTime,
                quorum = notaryConsumers.size
            )
        }
        //Wait until Iroha commits MST
        Thread.sleep(2_000)
        val notaryAccount = notaryConsumers.first().creator
        val details = integrationHelperUtil.getAccountDetails(notaryAccount, notaryAccount)
        assertEquals(testValue, details[testKey])
    }


    /**
     * @given notary account with quorum set to 3
     * @when transaction is sent only once using MultisigIrohaConsumer
     * @then it's not committed in Iroha and test thread is not blocked
     */
    @Test
    fun testNotCommitted() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val testKey = "key_2"
            val testValue = "value_2"
            val consumer = notaryConsumers.first()
            ModelUtil.setAccountDetail(
                irohaConsumer = consumer,
                accountId = consumer.creator,
                key = testKey,
                value = testValue,
                createdTime = System.currentTimeMillis(),
                quorum = notaryConsumers.size
            )
            //Wait until Iroha commits MST
            Thread.sleep(2_000)
            val notaryAccount = notaryConsumers.first().creator
            val details = integrationHelperUtil.getAccountDetails(notaryAccount, notaryAccount)
            assertNull(details[testKey])
        }
    }
}
