package integration

import config.loadConfigs
import io.reactivex.schedulers.Schedulers
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
import notary.NotaryConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.toByteVector
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals


class IrohaBatchTest {
    val testConfig by lazy {
        loadConfigs("test", NotaryConfig::class.java)
    }

    val tester = "test@notary"
    val keypair by lazy {
        ModelUtil.loadKeypair(testConfig.iroha.pubkeyPath, testConfig.iroha.privkeyPath).get()
    }

    val counter = BigInteger.ONE

    val channel by lazy {
        ModelUtil.getChannel(testConfig.iroha.hostname, testConfig.iroha.port)
    }
    val queryStub by lazy {
        ModelUtil.getQueryStub(channel)
    }

    @Test
    fun batchTest() {

        val irohaConsumer = IrohaConsumerImpl(testConfig.iroha)

        val txList =
            listOf(
                IrohaTransaction(
                    tester,
                    listOf(
                        IrohaCommand.CommandCreateAccount(
                            "u1",
                            "notary",
                            ModelCrypto().generateKeypair().publicKey().hex()
                        )
                    )
                ),
                IrohaTransaction(
                    tester,
                    listOf(
                        IrohaCommand.CommandSetAccountDetail(
                            "u1@notary",
                            "key",
                            "value"
                        )
                    )
                ),
                IrohaTransaction(
                    tester,
                    listOf(
                        IrohaCommand.CommandCreateAsset(
                            "batches",
                            "notary",
                            0
                        ),
                        IrohaCommand.CommandAddAssetQuantity(
                            "batches#notary",
                            "100"
                        ),
                        IrohaCommand.CommandTransferAsset(
                            tester,
                            "u1@notary",
                            "batches#notary",
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
        var blockHashes: List<String>? = null

        IrohaChainListener(
            testConfig.iroha.hostname,
            testConfig.iroha.port,
            tester, keypair
        ).getBlockObservable().get().map { block ->
            val txs = block.payload.transactionsList

            blockHashes = txs.map {
                Blob(iroha.hashTransaction(it.toByteArray().toByteVector())).hex()
            }
        }.subscribeOn(Schedulers.io()).subscribe()


        val successHash = irohaConsumer.sendAndCheck(lst).get()


        var uquery = ModelQueryBuilder()
            .creatorAccountId(tester)
            .queryCounter(counter)
            .createdTime(ModelUtil.getCurrentTime())
            .getAccount("u1@notary")
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
            .getAssetInfo("batches#notary")
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
            .getAccountAssets("u1@notary")
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
        assertEquals(account.accountId, "u1@notary")
        assertEquals(account.jsonData, "{\"test@notary\": {\"key\": \"value\"}}")
        assertEquals(asset.assetId, "batches#notary")
        assertEquals(tester_amount.toInt(), 73)
        assertEquals(u1_amount.toInt(), 27)

        val scope = async {
            while (blockHashes == null);
            assertEquals(blockHashes, hashes)

        }
        runBlocking {
            withTimeout(10, TimeUnit.SECONDS) { scope.await() }
        }
    }
}

