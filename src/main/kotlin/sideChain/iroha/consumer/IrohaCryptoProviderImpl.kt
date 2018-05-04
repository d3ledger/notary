package sideChain.iroha.consumer

import Keypair
import ModelCrypto
import java.nio.file.Paths

/**
 * Provides with keypair loaded from files
 * @param pubkeyPath path to file with public key
 * @param privkeyPath path to file with private key
 */
class IrohaCryptoProviderImpl(pubkeyPath: String, privkeyPath: String) : IrohaCryptoProvider {

    /** Iroha keypair */
    override val keypair: Keypair

    init {
        // Load keypair from existing files
        val crypto = ModelCrypto()
        keypair = crypto.convertFromExisting(
            readKeyFromFile(pubkeyPath),
            readKeyFromFile(privkeyPath)
        )
    }

    /**
     * Read key from file specified by [path]
     */
    private fun readKeyFromFile(path: String): String {
        try {
            return String(java.nio.file.Files.readAllBytes(Paths.get(path)))
        } catch (e: java.io.IOException) {
            IrohaConsumerImpl.logger.error { "Unable to read key files.\n $e" }
            System.exit(1)
        }
        return ""
    }
}
