package integration.iroha

import integration.helper.IrohaConfigHelper
import integration.helper.IrohaIntegrationHelperUtil
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.Utils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import notary.IrohaCommand
import notary.IrohaOrderedBatch
import notary.IrohaTransaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import sidechain.iroha.CLIENT_DOMAIN
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverter
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.getAccountAsset
import sidechain.iroha.util.getAccountData
import util.getRandomString
import util.hex
import util.toHexString
import java.math.BigInteger
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val BATCH_TIME_WAIT = 5000L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IrohaBatchTest {

    private val integrationHelper = IrohaIntegrationHelperUtil()

    val testConfig = integrationHelper.testConfig

    private val testCredential = integrationHelper.testCredential

    private val tester = testCredential.accountId

    val assetDomain = "notary"

    val listener = IrohaChainListener(
        testConfig.iroha.hostname,
        testConfig.iroha.port,
        testCredential
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
        Assertions.assertTimeoutPreemptively(timeoutDuration) {

            val user = randomString()
            val asset_name = randomString()

            val irohaConsumer = IrohaConsumerImpl(testCredential, integrationHelper.irohaAPI)

            val userId = "$user@$CLIENT_DOMAIN"

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
                                CLIENT_DOMAIN,
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
                listener.getBlock().blockV1.payload.transactionsList.map {
                    String.hex(Utils.hash(it))
                }
            }

            val successHash = irohaConsumer.send(lst).get()

            Thread.sleep(BATCH_TIME_WAIT)

            val accountJson = getAccountData(integrationHelper.queryAPI, userId).get().toJsonString()
            val tester_amount = getAccountAsset(integrationHelper.queryAPI, tester, "$asset_name#$assetDomain").get()
            val u1_amount =
                getAccountAsset(integrationHelper.queryAPI, userId, "$asset_name#$assetDomain").get()

            assertEquals(hashes.size, successHash.size)
            assertTrue(successHash.containsAll(hashes))
            assertEquals("{\"$tester\":{\"key\":\"value\"}}", accountJson)
            assertEquals(73, tester_amount.toInt())
            assertEquals(27, u1_amount.toInt())

            runBlocking {
                withTimeout(10_000) {
                    assertEquals(hashes, blockHashes.await())
                }
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
        Assertions.assertTimeoutPreemptively(timeoutDuration) {

            val user = randomString()
            val asset_name = randomString()

            val irohaConsumer = IrohaConsumerImpl(testCredential, integrationHelper.irohaAPI)

            val userId = "$user@$CLIENT_DOMAIN"
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
                                CLIENT_DOMAIN,
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
                listener.getBlock().blockV1.payload.transactionsList.map {
                    String.hex(Utils.hash(it))
                }
            }

            val successHash = irohaConsumer.send(lst).get()

            Thread.sleep(BATCH_TIME_WAIT)

            val accountJson = getAccountData(integrationHelper.queryAPI, userId).get().toJsonString()
            val tester_amount = getAccountAsset(integrationHelper.queryAPI, tester, assetId).get()
            val u1_amount =
                getAccountAsset(integrationHelper.queryAPI, userId, assetId).get()

            assertEquals(expectedHashes.size, successHash.size)
            assertTrue(successHash.containsAll(expectedHashes))
            assertEquals("{\"$tester\":{\"key\":\"value\"}}", accountJson)
            assertEquals(73, tester_amount.toInt())
            assertEquals(27, u1_amount.toInt())

            runBlocking {
                withTimeout(10_000) {
                    assertEquals(expectedHashes, blockHashes.await())
                }
            }
        }
    }
}
