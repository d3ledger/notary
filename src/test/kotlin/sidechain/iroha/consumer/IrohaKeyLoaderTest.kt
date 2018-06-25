package sidechain.iroha.consumer

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import sidechain.iroha.IrohaInitializtion


internal class IrohaKeyLoaderTest {

    /**
     * @given broken paths of keys
     * @when  try fetch keys in RAM
     * @then  result contains exception
     */
    @Test
    fun keyLoadFailedTest() {
        IrohaInitializtion.loadIrohaLibrary()
        val keypair = IrohaKeyLoader.loadKeypair("", "")

        assertNotNull(keypair.component2())
    }
}
