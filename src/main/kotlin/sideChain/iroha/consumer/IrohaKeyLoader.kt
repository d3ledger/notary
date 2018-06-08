package sideChain.iroha.consumer

import com.github.kittinunf.result.Result
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelCrypto
import java.io.IOException
import java.nio.file.Paths

/**
 * Provides with keypair loaded from files
 */
object IrohaKeyLoader {

    /**
     * Load keypair from existing files
     * @param pubkeyPath path to file with public key
     * @param privkeyPath path to file with private key
     */
    fun loadKeypair(pubkeyPath: String, privkeyPath: String): Result<Keypair, Exception> {
        val crypto = ModelCrypto()
        return Result.of {
            try {
                crypto.convertFromExisting(
                    String(java.nio.file.Files.readAllBytes(Paths.get(pubkeyPath))),
                    String(java.nio.file.Files.readAllBytes(Paths.get(privkeyPath)))
                )
            } catch (e: IOException) {
                throw Exception("Unable to read Iroha key files. \n ${e.message}", e)
            }
        }
    }

}
