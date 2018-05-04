package sideChain.iroha

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
            System.err.println("Native code library failed to load. \n" + e)
            System.err.println("java.library.path=${System.getProperty("java.library.path")}")
            System.exit(1)
        }
    }
}
