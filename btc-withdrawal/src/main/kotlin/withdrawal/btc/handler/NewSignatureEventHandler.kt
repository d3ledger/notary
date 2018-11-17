package withdrawal.btc.handler

import iroha.protocol.Commands
import mu.KLogging
import org.bitcoinj.core.PeerGroup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import sidechain.iroha.BTC_SIGN_COLLECT_DOMAIN
import withdrawal.btc.transaction.SignCollector
import withdrawal.btc.transaction.UnsignedTransactions

/*
    Class that is used to handle new input signature appearance events
 */
@Component
class NewSignatureEventHandler(
    @Autowired private val signCollector: SignCollector,
    @Autowired private val unsignedTransactions: UnsignedTransactions
) {

    /**
     * Handles "add new input signatures" commands
     * @param addNewSignatureCommand - command object full of signatures
     * @param peerGroup - group of Bitcoin peers. Used to broadcast withdraw transactions.
     */
    fun handleNewSignatureCommand(addNewSignatureCommand: Commands.SetAccountDetail, peerGroup: PeerGroup) {
        val shortTxHash = addNewSignatureCommand.accountId.replace("@$BTC_SIGN_COLLECT_DOMAIN", "")
        val unsignedTx = unsignedTransactions.get(shortTxHash)
        if (unsignedTx == null) {
            logger.warn { "No tx starting with hash $shortTxHash was found in collection of unsigned transactions" }
            return
        }
        val tx = unsignedTx.tx
        // Hash of transaction will be changed after signing. This is why we keep an "original" hash
        val originalHash = tx.hashAsString
        signCollector.getSignatures(originalHash).fold({ signatures ->
            val enoughSignaturesCollected = signCollector.isEnoughSignaturesCollected(tx, signatures)
            if (!enoughSignaturesCollected) {
                logger.info { "Not enough signatures were collected for tx $originalHash" }
                return
            }
            logger.info { "Tx $originalHash has enough signatures" }
            signCollector.fillTxWithSignatures(tx, signatures)
                .fold({
                    peerGroup.broadcastTransaction(tx)
                    unsignedTransactions.remove(shortTxHash)
                }, { ex -> logger.error("Cannot complete tx $originalHash", ex) })


        }, { ex -> logger.error("Cannot get signatures for tx $originalHash", ex) })
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
