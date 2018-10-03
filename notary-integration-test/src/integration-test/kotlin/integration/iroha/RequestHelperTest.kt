package integration.iroha

import integration.helper.IntegrationHelperUtil
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import sidechain.iroha.consumer.IrohaNetworkImpl
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

    val helper = IntegrationHelperUtil()
    val credential = helper.testCredential
    val irohaConfig = helper.configHelper.createIrohaConfig()


    private val irohaNetwork = IrohaNetworkImpl(irohaConfig.hostname, irohaConfig.port)

    /**
     * @given Iroha running
     * @when query balance of nonexistent asset
     * @then return Result with exception
     */
    @Test
    fun getNonexistentAccountAssetTest() {
        val accountId = credential.accountId
        val assetId = "nonexist#nonexist"

        assertEquals("0", getAccountAsset(credential, irohaNetwork, accountId, assetId).get())
    }

}
