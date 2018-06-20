package sideChain.eth.consumer

import mu.KLogging
import withdrawalservice.WithdrawalServiceOutputEvent

class EthConsumer {

    // TODO a.chernyshov: rework with effective implementation
    fun consume(event: WithdrawalServiceOutputEvent) {
        logger.info { "consumed eth event" }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
