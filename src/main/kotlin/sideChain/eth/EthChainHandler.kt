package sideChain.eth

import main.CONFIG
import main.ConfigKeys
import mu.KLogging
import notary.NotaryEvent
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.Transaction
import sideChain.ChainHandler
import util.hexToAscii

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

        return block.block.transactions
            .map { it.get() as Transaction }
                .filter { it.to == CONFIG[ConfigKeys.ethListenAddress] }
            .map {
                NotaryEvent.EthChainEvent.OnEthSidechainTransfer(
                    it.hash,
                    it.from,
                    it.value,
                    it.input.drop(2).hexToAscii()
                )
            }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
