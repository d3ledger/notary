package sideChain.iroha.consumer

import Keypair

/**
 * Provides with Iroha keypair
 */
interface IrohaCryptoProvider {
    /** Iroha public and private key */
    val keypair: Keypair
}
