package com.d3.commons.sidechain.iroha.consumer

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import com.d3.commons.sidechain.iroha.util.ModelUtil

internal class IrohaKeyLoaderTest {

    /**
     * @given broken paths of keys
     * @when  try fetch keys in RAM
     * @then  result contains exception
     */
    @Test
    fun keyLoadFailedTest() {
        val keypair = ModelUtil.loadKeypair("", "")

        assertNotNull(keypair.component2())
    }
}
