package sideChain.iroha.consumer

import io.grpc.ManagedChannelBuilder
import iroha.protocol.BlockOuterClass
import iroha.protocol.CommandServiceGrpc
import notary.CONFIG
import main.ConfigKeys
import mu.KLogging

/**
 * Implements netwrok layer of Iroha chain
 */
class IrohaNetworkImpl : IrohaNetwork {

    /** Grpc stub for streaming output calls on the service */
    val toriiStub: iroha.protocol.CommandServiceGrpc.CommandServiceBlockingStub

    init {
        val channel =
            ManagedChannelBuilder.forAddress(CONFIG[ConfigKeys.irohaHostname], CONFIG[ConfigKeys.irohaPort])
                .usePlaintext(true).build()
        toriiStub = CommandServiceGrpc.newBlockingStub(channel)
    }

    /**
     * Send transaction to iroha
     * @param protoTx protobuf representation of transaction
     */
    override fun send(protoTx: BlockOuterClass.Transaction) {
        logger.info { "TX to IROHA" }

        // Send transaction to iroha
        toriiStub.torii(protoTx)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
