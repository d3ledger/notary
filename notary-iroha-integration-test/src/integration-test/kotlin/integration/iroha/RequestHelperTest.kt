package integration.iroha

import integration.helper.IrohaIntegrationHelperUtil
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import sidechain.iroha.util.getAccountAsset
import kotlin.test.assertEquals

/**
 * Test helper class for Iroha queries
 *
 * Note: Requires Iroha running.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequestHelperTest {

    val integrationHelper = IrohaIntegrationHelperUtil()
    val credential = integrationHelper.testCredential
    val irohaConfig = integrationHelper.configHelper.createIrohaConfig()

    private val irohaAPI = IrohaAPI(irohaConfig.hostname, irohaConfig.port)

    private val queryAPI = QueryAPI(irohaAPI, credential.accountId, credential.keyPair)


    @AfterAll
    fun dropDown() {
        integrationHelper.close()
        irohaAPI.close()
    }

    /**
     * @given Iroha running
     * @when query balance of nonexistent asset
     * @then return Result with exception
     */
    @Test
    fun getNonexistentAccountAssetTest() {
        val accountId = credential.accountId
        val assetId = "nonexist#nonexist"

        assertEquals("0", getAccountAsset(queryAPI, accountId, assetId).get())
    }
}
