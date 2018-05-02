package vendor

import ModelCrypto
import org.junit.Test

class IrohaUsageTest {

    /**
     * Load iroha binding library
     */
    fun loadLibrary() {
        try {
            println("Iroha path=${System.getProperty("java.library.path")}")
            System.loadLibrary("irohajava")
        } catch (e: UnsatisfiedLinkError) {
            System.err.println("Native code library failed to load. \n$e")
            System.exit(1)
        }
    }

    /**
     * Iroha usage test
     */
    @Test
    fun irohaUsage() {
        loadLibrary()

        val crypto = ModelCrypto()
        println(crypto.generateKeypair())
    }
}
