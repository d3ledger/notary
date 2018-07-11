package registration

import config.ConfigKeys
import notary.CONFIG
import notary.EthWalletsProviderIrohaImpl
import notary.IrohaCommand
import notary.IrohaTransaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil

/**
 * Requires Iroha is running
 */
class EthWalletsProviderIrohaTest {

    /**
     * @given
     * @when
     * @then
     */
    @Test
    fun testParsing() {
        val json = """
            {
                "data": {
                    "age": "22",
                    "surname": "Bit",
                    "name": "Connect"
                },
                "meta" : {
                    "author" : "notary",
                    "timestamp" : "123"
                }
            }
            """

        val parsed = ModelUtil.jsonToKV(json)!!

        assertEquals(parsed["data"]!!["age"], "22")
        assertEquals(parsed["data"]!!["surname"], "Bit")
        assertEquals(parsed["data"]!!["name"], "Connect")

        assertEquals(parsed["meta"]!!["author"], "notary")
        assertEquals(parsed["meta"]!!["timestamp"], "123")
    }

    /**
     * @given
     * @when
     * @then
     */
    @Disabled
    @Test
    fun storageTest() {
        IrohaInitialization.loadIrohaLibrary()

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

        val keypair = ModelUtil.loadKeypair(
            CONFIG[ConfigKeys.testPubkeyPath],
            CONFIG[ConfigKeys.testPrivkeyPath]
        )

        val creator = CONFIG[ConfigKeys.registrationServiceIrohaAccount]
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
        val tx = IrohaConsumerImpl(keypair.component1()!!).convertToProto(it)

        IrohaNetworkImpl(
            "localhost",
            CONFIG[ConfigKeys.registrationServiceIrohaPort]
        ).sendAndCheck(tx, hash)

        val lst = EthWalletsProviderIrohaImpl().getWallets()
        assertEquals(valid, lst.component1())
    }

    /**
     * @given
     * @when
     * @then
     */
    @Disabled
    @Test
    fun testEmptyStorage() {
        IrohaInitialization.loadIrohaLibrary()

        println(EthWalletsProviderIrohaImpl().getWallets().get())
    }
}
