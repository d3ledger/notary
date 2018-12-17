package integration.iroha

import integration.helper.IrohaConfigHelper
import integration.helper.IrohaIntegrationHelperUtil
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.IrohaAPI
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

private const val BATCH_TIME_WAIT = 5000L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IrohaBatchTest {

    private val integrationHelper = IrohaIntegrationHelperUtil()

    val testConfig = integrationHelper.testConfig

    private val testCredential = integrationHelper.testCredential

    private val tester = testCredential.accountId

    val assetDomain = "notary"

    private val irohaAPI = IrohaAPI(testConfig.iroha.hostname, testConfig.iroha.port)

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
        irohaAPI.close()
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

            val irohaConsumer = IrohaConsumerImpl(testCredential, irohaAPI)

            val userId = "$user@$CLIENT_DOMAIN"

            val createdTime = ModelUtil.getCurrentTime()
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
                        createdTime.add(BigInteger.ONE),
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
                        createdTime.add(BigInteger.TEN),
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
            val lst = IrohaConverter.convert(batch, irohaConsumer.creator)
            val hashes = lst.map { String.hex(it.hash()) }

            val blockHashes = GlobalScope.async {
                listener.getBlock().blockV1.payload.transactionsList.map {
                    Utils.hash(it)
                }
            }

            val successHash = irohaConsumer.send(lst).get().map { String.hex(it) }

            Thread.sleep(BATCH_TIME_WAIT)

            val accountJson = getAccountData(irohaAPI, testCredential, userId).get().toJsonString()
            val tester_amount = getAccountAsset(irohaAPI, testCredential, tester, "$asset_name#$assetDomain").get()
            val u1_amount =
                getAccountAsset(irohaAPI, testCredential, userId, "$asset_name#$assetDomain").get()

            assertEquals(hashes, successHash)
            assertEquals("{\"$tester\":{\"key\":\"value\"}}", accountJson)
            assertEquals(73, tester_amount.toInt())
            assertEquals(27, u1_amount.toInt())

            runBlocking {
                withTimeout(10_000) {
                    assertEquals(hashes, blockHashes.await().map { String.hex(it) })
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

            val irohaConsumer = IrohaConsumerImpl(testCredential, irohaAPI)

            val userId = "$user@$CLIENT_DOMAIN"
            val assetId = "$asset_name#$assetDomain"

            val createdTime = ModelUtil.getCurrentTime()
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
                        createdTime.add(BigInteger.ONE),
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
                        createdTime.add(BigInteger.TEN),
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
                        createdTime.add(BigInteger.TEN).add(BigInteger.TEN),
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
            val lst = IrohaConverter.convert(batch, irohaConsumer.creator)
            val hashes = lst.map { String.hex(it.hash()) }
            val expectedHashes = hashes.subList(0, hashes.size - 1)

            val blockHashes = GlobalScope.async {
                listener.getBlock().blockV1.payload.transactionsList.map {
                    Utils.hash(it)
                }
            }

            val successHash = irohaConsumer.send(lst).get().map { String.hex(it) }

            Thread.sleep(BATCH_TIME_WAIT)

            val accountJson = getAccountData(irohaAPI, testCredential, userId).get().toJsonString()
            val tester_amount = getAccountAsset(irohaAPI, testCredential, tester, assetId).get()
            val u1_amount =
                getAccountAsset(irohaAPI, testCredential, userId, assetId).get()

            assertEquals(expectedHashes, successHash)
            assertEquals("{\"$tester\":{\"key\":\"value\"}}", accountJson)
            assertEquals(73, tester_amount.toInt())
            assertEquals(27, u1_amount.toInt())

            runBlocking {
                withTimeout(10_000) {
                    assertEquals(expectedHashes, blockHashes.await().map { String.hex(it) })
                }
            }
        }
    }
}
