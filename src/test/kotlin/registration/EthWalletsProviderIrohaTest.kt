package registration

import com.github.kittinunf.result.failure
import config.ConfigKeys
import notary.CONFIG
import notary.EthWalletsProviderIrohaImpl
import notary.IrohaCommand
import notary.IrohaTransaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil

/**
 * Requires Iroha is running
 */
class EthWalletsProviderIrohaTest {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure {
                println(it)
                System.exit(1)
            }
    }

    /** Creator of txs in Iroha */
    val creator: String = CONFIG[ConfigKeys.testIrohaAccount]

    /** Iroha keypair */
    val keypair = ModelUtil.loadKeypair(
        CONFIG[ConfigKeys.testPubkeyPath],
        CONFIG[ConfigKeys.testPrivkeyPath]
    ).get()

    /** Iroha account that has set details */
    val detailSetter = CONFIG[ConfigKeys.registrationServiceIrohaAccount]

    /** Iroha account that holds details */
    val detailHolder = CONFIG[ConfigKeys.registrationServiceNotaryIrohaAccount]

    /**
     * @given [detailHolder] has ethereum wallets in details
     * @when getWallets() is called
     * @then not free wallets are returned in a map
     */
    @Disabled
    @Test
    fun storageTest() {
        val domain = "notary"

        val entries = mapOf(
            "0x281055afc982d96fab65b3a49cac8b878184cb16" to "user1@$domain",
            "0x6f46cf5569aefa1acc1009290c8e043747172d89" to "user2@$domain",
            "0x90e63c3d53e0ea496845b7a03ec7548b70014a91" to "user3@$domain",
            "0x53d284357ec70ce289d6d64134dfac8e511c8a3d" to "free",
            "0xab7c74abc0c4d48d1bdad5dcb26153fc8780f83e" to "user4@$domain",
            "0xfe9e8709d3215310075d67e3ed32a380ccf451c8" to "free"
        )

        val valid = entries.filter { it.value != "free" }

        val masterAccount = CONFIG[ConfigKeys.registrationServiceNotaryIrohaAccount]

        val irohaOutput = IrohaTransaction(
            creator,
            entries.map {
                // Set ethereum wallet as occupied by user id
                IrohaCommand.CommandSetAccountDetail(
                    masterAccount,
                    it.key,
                    it.value
                )
            }
        )

        val it = IrohaConverterImpl().convert(irohaOutput)
        val hash = it.hash()
        val tx = IrohaConsumerImpl(keypair).convertToProto(it)

        IrohaNetworkImpl(
            "localhost",
            CONFIG[ConfigKeys.registrationServiceIrohaPort]
        ).sendAndCheck(tx, hash)

        val lst = EthWalletsProviderIrohaImpl(creator, keypair, detailSetter, detailHolder).getWallets()
        assertEquals(valid, lst.component1())
    }

    /**
     * @given There is no account details on [detailHolder]
     * @when getWallets() is called
     * @then empty map is returned
     */
    @Disabled
    @Test
    fun testEmptyStorage() {
        EthWalletsProviderIrohaImpl(creator, keypair, detailSetter, detailHolder).getWallets().fold(
            {
                assert(it.isEmpty())
            },
            {
                fail { "result has exception ${it.toString()}" }
            }
        )
    }
}
