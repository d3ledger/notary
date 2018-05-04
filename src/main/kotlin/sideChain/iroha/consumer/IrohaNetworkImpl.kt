package sideChain.iroha.consumer

import io.grpc.ManagedChannelBuilder
import iroha.protocol.BlockOuterClass
import iroha.protocol.CommandServiceGrpc
import main.Configs
import mu.KLogging

/**
 * Implements netwrok layer of Iroha chain
 */
class IrohaNetworkImpl : IrohaNetwork {

    /**
     * Send transaction to iroha
     * @param protoTx protobuf representation of transaction
     */
    override fun send(protoTx: BlockOuterClass.Transaction) {
        logger.info { "TX to IROHA" }

        // Send transaction to iroha
        val channel =
            ManagedChannelBuilder.forAddress(Configs.irohaHostname, Configs.irohaPort).usePlaintext(true).build()
        val stub = CommandServiceGrpc.newBlockingStub(channel)
        stub.torii(protoTx)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
