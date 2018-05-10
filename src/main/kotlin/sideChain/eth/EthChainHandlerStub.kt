package sideChain.eth

import notary.NotaryEvent
import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import mu.KLogging
import org.web3j.protocol.core.methods.response.EthBlock
import sideChain.ChainHandler
import sideChain.ChainListener

/**
 * Dummy implementation of [ChainHandler] with effective dependencies
 */
class EthChainHandlerStub : ChainHandler<EthBlock> {

    /**
     * TODO Replace dummy with effective implementation
     */
    override fun parseBlock(block: EthBlock): NotaryEvent {
        logger.info { "Eth chain handler" }
        println("handler got block #${block.block.number}")
        return mock<NotaryEvent.EthChainEvent.OnEthSidechainTransfer>()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
