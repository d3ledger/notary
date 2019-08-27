/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.iroha

import com.d3.chainadapter.client.RMQConfig
import com.d3.chainadapter.client.ReliableIrohaChainListener
import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.notary.IrohaCommand
import com.d3.commons.notary.IrohaOrderedBatch
import com.d3.commons.notary.IrohaTransaction
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.consumer.IrohaConverter
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomId
import com.d3.commons.util.getRandomString
import com.d3.commons.util.hex
import com.d3.commons.util.toHexString
import integration.helper.D3_DOMAIN
import integration.helper.IrohaConfigHelper
import integration.helper.IrohaIntegrationHelperUtil
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.Utils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val BATCH_TIME_WAIT = 5000L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IrohaBatchTest {

    private val integrationHelper = IrohaIntegrationHelperUtil()

    private val testCredential = integrationHelper.testCredential

    private val tester = testCredential.accountId
    private val rmqConfig =
        loadRawLocalConfigs("rmq", RMQConfig::class.java, "rmq.properties")

    val assetDomain = "notary"

    private val listener = ReliableIrohaChainListener(
        rmqConfig,
        String.getRandomId()
    )

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    private fun randomString() = String.getRandomString(10)

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
        listener.close()
    }

    /**
     * @given A batch
     * @when All transactions are valid and sent as a batch
     * @then They all are accepted and committed in the same block
     */
    @Test
    fun allValidBatchTest() {
        Thread.sleep(5000)
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            listener.purge()

            val user = randomString()
            val asset_name = randomString()

            val irohaConsumer = IrohaConsumerImpl(testCredential, integrationHelper.irohaAPI)

            val userId = "$user@$D3_DOMAIN"

            val createdTime = ModelUtil.getCurrentTime().minus(BigInteger.valueOf(10_000))
            val txList =
                listOf(
                    IrohaTransaction(
                        tester,
                        createdTime,
                        1,
                        listOf(
                            IrohaCommand.CommandCreateAccount(
                                user,
                                D3_DOMAIN,
                                Ed25519Sha3().generateKeypair().public.toHexString()
                            )
                        )
                    ),
                    IrohaTransaction(
                        tester,
                        createdTime.add(BigInteger.valueOf(1000)),
                        1,
                        listOf(
                            IrohaCommand.CommandSetAccountDetail(
                                userId,
                                "key",
                                "value"
                            )
                        )
                    ),
                    IrohaTransaction(
                        tester,
                        createdTime.add(BigInteger.valueOf(2000)),
                        1,
                        listOf(
                            IrohaCommand.CommandCreateAsset(
                                asset_name,
                                assetDomain,
                                0
                            ),
                            IrohaCommand.CommandAddAssetQuantity(
                                "$asset_name#$assetDomain",
                                "100"
                            ),
                            IrohaCommand.CommandTransferAsset(
                                tester,
                                userId,
                                "$asset_name#$assetDomain",
                                "desc",
                                "27"
                            )
                        )
                    )

                )

            val batch = IrohaOrderedBatch(txList)
            val lst = IrohaConverter.convert(batch, testCredential.keyPair)
            val hashes = lst.map { String.hex(Utils.hash(it)) }

            val blockHashes = GlobalScope.async {
                withTimeout(10_000) {
                    val (block, ack) = listener.getBlock()
                    ack()
                    block.blockV1.payload.transactionsList.map {
                        String.hex(Utils.hash(it))
                    }
                }
            }

            listener.purge()
            val successHash = irohaConsumer.send(lst).get()

            val accountDetail =
                integrationHelper.queryHelper.getAccountDetails(userId, tester).get()
            val tester_amount = integrationHelper.queryHelper.getAccountAsset(
                tester,
                "$asset_name#$assetDomain"
            ).get()
            val u1_amount = integrationHelper.queryHelper.getAccountAsset(
                userId,
                "$asset_name#$assetDomain"
            ).get()

            assertEquals(hashes.size, successHash.size)
            assertTrue(successHash.containsAll(hashes))
            assertEquals(mapOf("key" to "value"), accountDetail)
            assertEquals(73, tester_amount.toInt())
            assertEquals(27, u1_amount.toInt())

            runBlocking {
                assertEquals(hashes, blockHashes.await())
            }
        }
    }

    /**
     * @given A batch
     * @when Not all transactions are valid and sent as a batch
     * @then Only valid are accepted and committed in the same block
     */
    @Test
    fun notAllValidBatchTest() {
        Thread.sleep(5000)
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            listener.purge()

            val user = randomString()
            val asset_name = randomString()

            val irohaConsumer = IrohaConsumerImpl(testCredential, integrationHelper.irohaAPI)

            val userId = "$user@$D3_DOMAIN"
            val assetId = "$asset_name#$assetDomain"

            val createdTime = ModelUtil.getCurrentTime().minus(BigInteger.valueOf(10_000))
            val txList =
                listOf(
                    IrohaTransaction(
                        tester,
                        createdTime,
                        1,
                        listOf(
                            IrohaCommand.CommandCreateAccount(
                                user,
                                D3_DOMAIN,
                                Ed25519Sha3().generateKeypair().public.toHexString()
                            )
                        )
                    ),
                    IrohaTransaction(
                        tester,
                        createdTime.add(BigInteger.valueOf(1000)),
                        1,
                        listOf(
                            IrohaCommand.CommandSetAccountDetail(
                                userId,
                                "key",
                                "value"
                            )
                        )
                    ),
                    IrohaTransaction(
                        tester,
                        createdTime.add(BigInteger.valueOf(2000)),
                        1,
                        listOf(
                            IrohaCommand.CommandCreateAsset(
                                asset_name,
                                assetDomain,
                                0
                            ),
                            IrohaCommand.CommandAddAssetQuantity(
                                assetId,
                                "100"
                            ),
                            IrohaCommand.CommandTransferAsset(
                                tester,
                                userId,
                                assetId,
                                "desc",
                                "27"
                            )
                        )
                    ),
                    IrohaTransaction(
                        tester,
                        createdTime.add(BigInteger.valueOf(3000)),
                        1,
                        listOf(
                            IrohaCommand.CommandTransferAsset(
                                tester,
                                userId,
                                assetId,
                                "",
                                "1234"
                            )
                        )
                    )

                )

            val batch = IrohaOrderedBatch(txList)
            val lst = IrohaConverter.convert(batch, testCredential.keyPair)
            val hashes = lst.map { String.hex(Utils.hash(it)) }
            val expectedHashes = hashes.subList(0, hashes.size - 1)

            val blockHashes = GlobalScope.async {
                withTimeout(10_000) {
                    val (block, ack) = listener.getBlock()
                    ack()
                    block.blockV1.payload.transactionsList.map {
                        String.hex(Utils.hash(it))
                    }
                }
            }

            listener.purge()
            val successHash = irohaConsumer.send(lst).get()

            Thread.sleep(BATCH_TIME_WAIT)

            val accountDetail =
                integrationHelper.queryHelper.getAccountDetails(userId, tester).get()
            val tester_amount = integrationHelper.queryHelper.getAccountAsset(tester, assetId).get()
            val u1_amount = integrationHelper.queryHelper.getAccountAsset(userId, assetId).get()

            assertEquals(expectedHashes.size, successHash.size)
            assertTrue(successHash.containsAll(expectedHashes))
            assertEquals(mapOf("key" to "value"), accountDetail)
            assertEquals(73, tester_amount.toInt())
            assertEquals(27, u1_amount.toInt())

            runBlocking {
                assertEquals(expectedHashes, blockHashes.await())
            }
        }
    }
}
