package sideChain.iroha

import Keypair
import UnsignedQuery
import UnsignedTx

/**
 * Class holds keypair and sign data
 */
class IrohaSigner(private val keyPair: Keypair) {

    /**
     * Sign transaction
     * @param tx - prepared transaction for signing
     */
    fun signTx(tx: UnsignedTx) = {
        tx.signAndAddSignature(keyPair)
    }

    /**
     * Sign query
     * @param query - prepared object for signing
     */
    fun signQuery(query: UnsignedQuery) = {
        query.signAndAddSignature(keyPair)
    }

}
