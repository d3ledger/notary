package sidechain.iroha.consumer

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

internal class IrohaKeyLoaderTest {

    /**
     * @given broken paths of keys
     * @when  try fetch keys in RAM
     * @then  result contains exception
     */
    @Test
    fun keyLoadFailedTest() {
        IrohaInitialization.loadIrohaLibrary()
        val keypair = ModelUtil.loadKeypair("", "")

        assertNotNull(keypair.component2())
    }
}
