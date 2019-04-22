package integration.iroha

import integration.helper.IrohaIntegrationHelperUtil
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Test helper class for Iroha queries
 *
 * Note: Requires Iroha running.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IrohaQueryHelperTest {

    val integrationHelper = IrohaIntegrationHelperUtil()

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
    }

    /**
     * @given Iroha running
     * @when query balance of nonexistent asset
     * @then return Result with exception
     */
    @Test
    fun getNonexistentAccountAssetTest() {
        val accountId = integrationHelper.testCredential.accountId
        val assetId = "nonexist#nonexist"

        assertEquals("0", integrationHelper.queryHelper.getAccountAsset(accountId, assetId).get())
    }
}
