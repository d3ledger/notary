/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.iroha

import com.d3.commons.config.RMQConfig
import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.sidechain.iroha.ReliableIrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.util.getRandomId
import integration.helper.IrohaConfigHelper
import integration.helper.IrohaIntegrationHelperUtil
import jp.co.soramitsu.iroha.java.Transaction
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.Duration

/**
 * Note: Requires Iroha is running.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IrohaBlockStreamingTest {

    private val integrationHelper = IrohaIntegrationHelperUtil()

    private val testCredential = integrationHelper.testCredential

    private val creator = testCredential.accountId

    private val rmqConfig =
        loadRawLocalConfigs("rmq", RMQConfig::class.java, "rmq.properties")
    private lateinit var listener: ReliableIrohaChainListener

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    @BeforeEach
    fun setUp() {
        listener = ReliableIrohaChainListener(
            rmqConfig,
            String.getRandomId()
        )
    }

    @AfterEach
    fun dropDown() {
        listener.close()
    }

    @AfterAll
    fun tearDown() {
        integrationHelper.close()
    }

    /**
     * @given Iroha running
     * @when new tx is sent to Iroha
     * @then block arrived to IrohaListener
     */
    @Test
    fun irohaStreamingTest() {
        Thread.sleep(5000)
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val cmds = mutableListOf<iroha.protocol.Commands.Command>()
            listener.getBlockObservable().get().subscribe { (block, _) ->
                cmds.addAll(
                    block.blockV1.payload.transactionsList
                        .flatMap {
                            it.payload.reducedPayload.commandsList
                        }
                )
            }
            listener.purge()
            listener.listen()
            val utx = Transaction.builder(creator)
                .setAccountDetail(creator, "test", "test")
                .build()

            IrohaConsumerImpl(testCredential, integrationHelper.irohaAPI).send(utx)
            runBlocking {
                delay(5000)
            }

            assertEquals(creator, cmds.last().setAccountDetail.accountId)
            assertEquals("test", cmds.last().setAccountDetail.key)
            assertEquals("test", cmds.last().setAccountDetail.value)
        }
    }

    /**
     * @given Iroha running
     * @when new tx is sent to Iroha
     * @then block arrived to IrohaListener and returned as coroutine
     */
    @Test
    fun irohaGetBlockTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val block = GlobalScope.async {
                listener.getBlock()
            }

            val utx = Transaction.builder(creator)
                .setAccountDetail(creator, "test", "test")
                .build()

            IrohaConsumerImpl(testCredential, integrationHelper.irohaAPI).send(utx)


            val (bl, _) = runBlocking {
                block.await()
            }

            val cmds = bl.blockV1.payload.transactionsList
                .flatMap {
                    it.payload.reducedPayload.commandsList
                }
            assertEquals(1, cmds.size)
            assertEquals(creator, cmds.first().setAccountDetail.accountId)
            assertEquals("test", cmds.first().setAccountDetail.key)
            assertEquals("test", cmds.first().setAccountDetail.value)
        }
    }
}
