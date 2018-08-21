package integration

import config.loadConfigs
import jp.co.soramitsu.iroha.ModelCrypto
import notary.IrohaCommand
import notary.IrohaOrderedBatch
import notary.IrohaTransaction
import notary.NotaryConfig
import org.junit.jupiter.api.Test
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverterImpl

class IrohaBatchTest {
    @Test
    fun batchTest() {
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
        val result = irohaConsumer.sendAndCheck(lst)
        println(result.get())
    }
}
