package integration

import config.loadConfigs
import jp.co.soramitsu.iroha.ModelCrypto
import jp.co.soramitsu.iroha.ModelQueryBuilder
import notary.IrohaCommand
import notary.IrohaOrderedBatch
import notary.IrohaTransaction
import notary.NotaryConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.util.ModelUtil
import java.math.BigInteger
import kotlin.test.assertEquals

class IrohaBatchTest {
    @Test
    fun batchTest() {
        System.loadLibrary("irohajava")
        val tester = "test@notary"
        val notaryConfig = loadConfigs("test", NotaryConfig::class.java)

        val irohaConsumer = IrohaConsumerImpl(notaryConfig.iroha)

        val batch = IrohaOrderedBatch(
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
                        )
                    )
                ),
                IrohaTransaction(
                    tester,
                    listOf(
                        IrohaCommand.CommandAddAssetQuantity(
                            "batches#notary",
                            "100"
                        )
                    )
                ),
                IrohaTransaction(
                    tester,
                    listOf(
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
        )
        val lst = IrohaConverterImpl().convert(batch)
        irohaConsumer.sendAndCheck(lst)


        val counter = BigInteger.ONE
        val keypair = ModelUtil.loadKeypair(notaryConfig.iroha.pubkeyPath, notaryConfig.iroha.privkeyPath).get()

        val channel = ModelUtil.getChannel(notaryConfig.iroha.hostname, notaryConfig.iroha.port)
        val queryStub = ModelUtil.getQueryStub(channel)

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

        assertEquals(account.accountId, "u1@notary")
        assertEquals(account.jsonData, "{\"test@notary\": {\"key\": \"value\"}}")
        assertEquals(asset.assetId, "batches#notary")
        assertEquals(tester_amount.toInt(), 73)
        assertEquals(u1_amount.toInt(), 27)
    }
}
