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

        return block.block.transactions
            .map { it.get() as Transaction }
            .filter { it.to == Configs.ethListenAddress }
            .map { NotaryEvent.EthChainEvent.OnEthSidechainTransfer(it.hash, it.from, it.value, it.input) }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
