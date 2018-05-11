package sideChain.eth

import main.Configs
import mu.KLogging
import notary.NotaryEvent
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.Transaction
import sideChain.ChainHandler

/**
 * Implementation of [ChainHandler] for Ethereum side chain.
 * Extract interesting transactions from Ethereum block.
 */
class EthChainHandler : ChainHandler<EthBlock> {

    /**
     * Parse [EthBlock] for transactions.
     * @return List of transation we are interested in
     */
    override fun parseBlock(block: EthBlock): List<NotaryEvent> {
        logger.info { "Eth chain handler for block ${block.block.number}" }

        val res = mutableListOf<NotaryEvent>()
        val txs = block.block.transactions

        for (txRes in txs) {
            val tx = txRes.get() as Transaction

            // we are interested in transactions to a particular address
            if (tx.to == Configs.ethListenAddress)
                res.add(NotaryEvent.EthChainEvent.OnEthSidechainTransfer(tx.hash, tx.from, tx.value, tx.input))
        }

        return res
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
