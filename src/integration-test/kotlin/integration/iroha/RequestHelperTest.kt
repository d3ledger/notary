package integration.iroha

import config.TestConfig
import config.loadConfigs
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.getAccountAsset
import kotlin.test.assertEquals

/**
 * Test helper class for Iroha queries
 *
 * Note: Requires Iroha running.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequestHelperTest {

    init {
        System.loadLibrary("irohajava")
    }

    /** Test configurations */
    val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties")

    val creator = testConfig.iroha.creator

    private val irohaNetwork = IrohaNetworkImpl(testConfig.iroha.hostname, testConfig.iroha.port)

    val keypair = ModelUtil.loadKeypair(
        testConfig.iroha.pubkeyPath,
        testConfig.iroha.privkeyPath
    ).get()

    /**
     * @given Iroha running
     * @when query balance of nonexistent asset
     * @then return Result with exception
     */
    @Test
    fun getNonexistentAccountAssetTest() {
        val accountId = testConfig.iroha.creator
        val assetId = "nonexist#nonexist"

        assertEquals("0", getAccountAsset(testConfig.iroha, keypair, irohaNetwork, accountId, assetId).get())
    }

}
