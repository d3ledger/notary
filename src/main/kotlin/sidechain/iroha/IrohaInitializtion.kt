package sidechain.iroha

import com.github.kittinunf.result.Result

/**
 * Initialize Iroha library
 */
object IrohaInitializtion {

    /**
     * Load iroha binding library
     */
    fun loadIrohaLibrary(): Result<Unit, Exception> {
        return Result.of {
            try {
                System.loadLibrary("irohajava")
            } catch (e: UnsatisfiedLinkError) {
                throw Exception(
                    """
Native code library failed to load. $e\n
java.library.path=${System.getProperty("java.library.path")}
                    """
                )
            }
        }
    }
}
