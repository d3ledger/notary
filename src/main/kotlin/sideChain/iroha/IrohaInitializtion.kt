package sideChain.iroha

import mu.KLogging

/**
 * Initialize Iroha library
 */
class IrohaInitializtion {

    /**
     * Load iroha binding library
     */
    fun loadIrohaLibrary() {
        try {
            System.loadLibrary("irohajava")
        } catch (e: UnsatisfiedLinkError) {
            logger.error { "Native code library failed to load. $e\n" }
            logger.error { "java.library.path=${System.getProperty("java.library.path")}" }
            System.exit(1)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
