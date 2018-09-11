package integration.iroha

import config.loadConfigs
import jp.co.soramitsu.iroha.Blob
import jp.co.soramitsu.iroha.ModelCrypto
import jp.co.soramitsu.iroha.ModelQueryBuilder
import jp.co.soramitsu.iroha.iroha
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeout
import notary.IrohaCommand
import notary.IrohaOrderedBatch
import notary.IrohaTransaction
import notary.eth.EthNotaryConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.toByteVector
import util.getRandomString
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class IrohaBatchTest {
    val testConfig by lazy {
        loadConfigs("test", EthNotaryConfig::class.java)
    }

    private val tester = "test@notary"

    private val keypair by lazy {
        ModelUtil.loadKeypair(testConfig.iroha.pubkeyPath, testConfig.iroha.privkeyPath).get()
    }

    private val counter = BigInteger.ONE

    private val channel by lazy {
        ModelUtil.getChannel(testConfig.iroha.hostname, testConfig.iroha.port)
    }

    private val queryStub by lazy {
        ModelUtil.getQueryStub(channel)
    }

    @BeforeEach
    fun load() {
        System.loadLibrary("irohajava")
    }

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

        val irohaConsumer = IrohaConsumerImpl(testConfig.iroha)

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
            listener.getBlock().payload.transactionsList.map {
                Blob(iroha.hashTransaction(it.toByteArray().toByteVector())).hex()
            }
        }


        val successHash = irohaConsumer.sendAndCheck(lst).get()


        var uquery = ModelQueryBuilder()
            .creatorAccountId(tester)
            .queryCounter(counter)
            .createdTime(ModelUtil.getCurrentTime())
            .getAccount("$user@notary")
            .build()

        val account = ModelUtil.prepareQuery(uquery, keypair)
            .fold(
                { protoQuery ->
                    val queryResponse = queryStub.find(protoQuery)
                    queryResponse.accountResponse.account
                },
                {
                    fail { "Exception while converting byte array to protobuf:" + it.message }
                }
            )

        uquery = ModelQueryBuilder()
            .creatorAccountId(tester)
            .queryCounter(counter)
            .createdTime(ModelUtil.getCurrentTime())
            .getAssetInfo("$asset_name#notary")
            .build()

        val asset = ModelUtil.prepareQuery(uquery, keypair)
            .fold(
                { protoQuery ->
                    val queryResponse = queryStub.find(protoQuery)
                    queryResponse.assetResponse.asset
                },
                {
                    fail { "Exception while converting byte array to protobuf:" + it.message }
                }
            )

        uquery = ModelQueryBuilder()
            .creatorAccountId(tester)
            .queryCounter(counter)
            .createdTime(ModelUtil.getCurrentTime())
            .getAccountAssets(tester)
            .build()

        val tester_amount = ModelUtil.prepareQuery(uquery, keypair)
            .fold(
                { protoQuery ->
                    val queryResponse = queryStub.find(protoQuery)
                    queryResponse.accountAssetsResponse.accountAssetsList.first().balance
                },
                {
                    fail { "Exception while converting byte array to protobuf:" + it.message }
                }
            )


        uquery = ModelQueryBuilder()
            .creatorAccountId(tester)
            .queryCounter(counter)
            .createdTime(ModelUtil.getCurrentTime())
            .getAccountAssets("$user@notary")
            .build()

        val u1_amount = ModelUtil.prepareQuery(uquery, keypair)
            .fold(
                { protoQuery ->
                    val queryResponse = queryStub.find(protoQuery)
                    queryResponse.accountAssetsResponse.accountAssetsList.first().balance
                },
                {
                    fail { "Exception while converting byte array to protobuf:" + it.message }
                }
            )


        assertEquals(hashes, successHash)
        assertEquals(account.accountId, "$user@notary")
        assertEquals(account.jsonData, "{\"test@notary\": {\"key\": \"value\"}}")
        assertEquals(asset.assetId, "$asset_name#notary")
        assertEquals(tester_amount.toInt(), 73)
        assertEquals(u1_amount.toInt(), 27)

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

        val irohaConsumer = IrohaConsumerImpl(testConfig.iroha)

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
            tester, keypair
        )
        val blockHashes = async {
            listener.getBlock().payload.transactionsList.map {
                Blob(iroha.hashTransaction(it.toByteArray().toByteVector())).hex()
            }
        }


        val successHash = irohaConsumer.sendAndCheck(lst).get()


        var uquery = ModelQueryBuilder()
            .creatorAccountId(tester)
            .queryCounter(counter)
            .createdTime(ModelUtil.getCurrentTime())
            .getAccount("$user@notary")
            .build()

        val account = ModelUtil.prepareQuery(uquery, keypair)
            .fold(
                { protoQuery ->
                    val queryResponse = queryStub.find(protoQuery)
                    queryResponse.accountResponse.account
                },
                {
                    fail { "Exception while converting byte array to protobuf:" + it.message }
                }
            )

        uquery = ModelQueryBuilder()
            .creatorAccountId(tester)
            .queryCounter(counter)
            .createdTime(ModelUtil.getCurrentTime())
            .getAssetInfo("$asset_name#notary")
            .build()

        val asset = ModelUtil.prepareQuery(uquery, keypair)
            .fold(
                { protoQuery ->
                    val queryResponse = queryStub.find(protoQuery)
                    queryResponse.assetResponse.asset
                },
                {
                    fail { "Exception while converting byte array to protobuf:" + it.message }
                }
            )

        uquery = ModelQueryBuilder()
            .creatorAccountId(tester)
            .queryCounter(counter)
            .createdTime(ModelUtil.getCurrentTime())
            .getAccountAssets(tester)
            .build()

        val tester_amount = ModelUtil.prepareQuery(uquery, keypair)
            .fold(
                { protoQuery ->
                    val queryResponse = queryStub.find(protoQuery)
                    queryResponse.accountAssetsResponse.accountAssetsList.first().balance
                },
                {
                    fail { "Exception while converting byte array to protobuf:" + it.message }
                }
            )


        uquery = ModelQueryBuilder()
            .creatorAccountId(tester)
            .queryCounter(counter)
            .createdTime(ModelUtil.getCurrentTime())
            .getAccountAssets("$user@notary")
            .build()

        val u1_amount = ModelUtil.prepareQuery(uquery, keypair)
            .fold(
                { protoQuery ->
                    val queryResponse = queryStub.find(protoQuery)
                    queryResponse.accountAssetsResponse.accountAssetsList.first().balance
                },
                {
                    fail { "Exception while converting byte array to protobuf:" + it.message }
                }
            )


        assertEquals(expectedHashes, successHash)
        assertEquals(account.accountId, "$user@notary")
        assertEquals(account.jsonData, "{\"test@notary\": {\"key\": \"value\"}}")
        assertEquals(asset.assetId, "$asset_name#notary")
        assertEquals(tester_amount.toInt(), 73)
        assertEquals(u1_amount.toInt(), 27)

        runBlocking {
            withTimeout(10, TimeUnit.SECONDS) {
                assertEquals(expectedHashes, blockHashes.await())
            }
        }
    }
}

