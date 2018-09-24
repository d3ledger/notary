package integration.iroha

import config.loadConfigs
import jp.co.soramitsu.iroha.Blob
import jp.co.soramitsu.iroha.ModelCrypto
import jp.co.soramitsu.iroha.iroha
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeout
import mu.KLogging
import notary.IrohaCommand
import notary.IrohaOrderedBatch
import notary.IrohaTransaction
import notary.eth.EthNotaryConfig
import org.junit.jupiter.api.Test
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.getAccountAsset
import sidechain.iroha.util.getAccountData
import sidechain.iroha.util.toByteVector
import util.getRandomString
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class IrohaBatchTest {

    init {
        System.loadLibrary("irohajava")
    }

    val testConfig by lazy {
        loadConfigs("test", EthNotaryConfig::class.java)
    }

    private val tester = "test@notary"

    private val keypair by lazy {
        ModelUtil.loadKeypair(testConfig.iroha.pubkeyPath, testConfig.iroha.privkeyPath).get()
    }

    private val irohaNetwork = IrohaNetworkImpl(testConfig.iroha.hostname, testConfig.iroha.port)

    private fun randomString() = String.getRandomString(10)

    /**
     * @given A batch
     * @when All transactions are valid and sent as a batch
     * @then They all are accepted and committed in the same block
     */
    @Test
    fun allValidBatchTest() {

        val user = randomString()
        val asset_name = randomString()

        val irohaConsumer = IrohaConsumerImpl(testConfig.iroha.creator, testConfig.iroha)

        val txList =
            listOf(
                IrohaTransaction(
                    tester,
                    ModelUtil.getCurrentTime(),
                    1,
                    listOf(
                        IrohaCommand.CommandCreateAccount(
                            user,
                            "notary",
                            ModelCrypto().generateKeypair().publicKey().hex()
                        )
                    )
                ),
                IrohaTransaction(
                    tester,
                    ModelUtil.getCurrentTime(),
                    1,
                    listOf(
                        IrohaCommand.CommandSetAccountDetail(
                            "$user@notary",
                            "key",
                            "value"
                        )
                    )
                ),
                IrohaTransaction(
                    tester,
                    ModelUtil.getCurrentTime(),
                    1,
                    listOf(
                        IrohaCommand.CommandCreateAsset(
                            asset_name,
                            "notary",
                            0
                        ),
                        IrohaCommand.CommandAddAssetQuantity(
                            "$asset_name#notary",
                            "100"
                        ),
                        IrohaCommand.CommandTransferAsset(
                            tester,
                            "$user@notary",
                            "$asset_name#notary",
                            "desc",
                            "27"
                        )
                    )
                )

            )

        val batch = IrohaOrderedBatch(
            txList
        )
        val lst = IrohaConverterImpl().convert(batch)
        val hashes = lst.map { it.hash().hex() }

        val listener = IrohaChainListener(
            testConfig.iroha.hostname,
            testConfig.iroha.port,
            tester, keypair
        )
        val blockHashes = async {
            val block = listener.getBlock()
            block.payload.transactionsList.map {
                Blob(iroha.hashTransaction(it.toByteArray().toByteVector())).hex()
            }
        }

        val successHash = irohaConsumer.sendAndCheck(lst).get()

        val accountJson = getAccountData(testConfig.iroha, keypair, irohaNetwork, "$user@notary").get().toJsonString()
        val tester_amount = getAccountAsset(testConfig.iroha, keypair, irohaNetwork, tester, "$asset_name#notary").get()
        val u1_amount =
            getAccountAsset(testConfig.iroha, keypair, irohaNetwork, "$user@notary", "$asset_name#notary").get()

        assertEquals(hashes, successHash)
        assertEquals("{\"test@notary\":{\"key\":\"value\"}}", accountJson)
        assertEquals(73, tester_amount.toInt())
        assertEquals(27, u1_amount.toInt())

        /////
        val tester_amount11 = getAccountAsset(testConfig.iroha, keypair, irohaNetwork, tester).get()
        val u1_amount11 = getAccountAsset(testConfig.iroha, keypair, irohaNetwork, "$user@notary").get()
        logger.info { "again ${tester} amount $tester_amount11" }
        logger.info { "again $$user@notary amount $u1_amount11" }
        /////

        runBlocking {
            withTimeout(10, TimeUnit.SECONDS) {
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
        val user = randomString()
        val asset_name = randomString()

        val irohaConsumer = IrohaConsumerImpl(testConfig.iroha.creator, testConfig.iroha)

        val txList =
            listOf(
                IrohaTransaction(
                    tester,
                    ModelUtil.getCurrentTime(),
                    1,
                    listOf(
                        IrohaCommand.CommandCreateAccount(
                            user,
                            "notary",
                            ModelCrypto().generateKeypair().publicKey().hex()
                        )
                    )
                ),
                IrohaTransaction(
                    tester,
                    ModelUtil.getCurrentTime(),
                    1,
                    listOf(
                        IrohaCommand.CommandSetAccountDetail(
                            "$user@notary",
                            "key",
                            "value"
                        )
                    )
                ),
                IrohaTransaction(
                    tester,
                    ModelUtil.getCurrentTime(),
                    1,
                    listOf(
                        IrohaCommand.CommandCreateAsset(
                            asset_name,
                            "notary",
                            0
                        ),
                        IrohaCommand.CommandAddAssetQuantity(
                            "$asset_name#notary",
                            "100"
                        ),
                        IrohaCommand.CommandTransferAsset(
                            tester,
                            "$user@notary",
                            "$asset_name#notary",
                            "desc",
                            "27"
                        )
                    )
                ),
                IrohaTransaction(
                    tester,
                    ModelUtil.getCurrentTime(),
                    1,
                    listOf(
                        IrohaCommand.CommandTransferAsset(
                            tester,
                            "$user@notary",
                            "$asset_name#notary",
                            "",
                            "1234"
                        )
                    )
                )

            )

        val batch = IrohaOrderedBatch(
            txList
        )
        val lst = IrohaConverterImpl().convert(batch)
        val hashes = lst.map { it.hash().hex() }
        val expectedHashes = hashes.subList(0, hashes.size - 1)

        val listener = IrohaChainListener(
            testConfig.iroha.hostname,
            testConfig.iroha.port,
            tester,
            keypair
        )
        val blockHashes = async {
            val block = listener.getBlock()
            block.payload.transactionsList.map {
                Blob(iroha.hashTransaction(it.toByteArray().toByteVector())).hex()
            }
        }

        val successHash = irohaConsumer.sendAndCheck(lst).get()
        val accountJson = getAccountData(testConfig.iroha, keypair, irohaNetwork, "$user@notary").get().toJsonString()
        val tester_amount = getAccountAsset(testConfig.iroha, keypair, irohaNetwork, tester, "$asset_name#notary").get()
        val u1_amount =
            getAccountAsset(testConfig.iroha, keypair, irohaNetwork, "$user@notary", "$asset_name#notary").get()

        /////
        val tester_amount11 = getAccountAsset(testConfig.iroha, keypair, irohaNetwork, tester).get()
        val u1_amount11 = getAccountAsset(testConfig.iroha, keypair, irohaNetwork, "$user@notary").get()
        logger.info { "again ${tester} amount $tester_amount11" }
        logger.info { "again $$user@notary amount $u1_amount11" }
        /////

        assertEquals(expectedHashes, successHash)
        assertEquals("{\"test@notary\":{\"key\":\"value\"}}", accountJson)
        assertEquals(73, tester_amount.toInt())
        assertEquals(27, u1_amount.toInt())

        runBlocking {
            withTimeout(10, TimeUnit.SECONDS) {
                assertEquals(expectedHashes, blockHashes.await())
            }
        }
    }

    companion object : KLogging()

}

