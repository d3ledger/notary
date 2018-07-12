package notary

import com.google.protobuf.ByteString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Test accessor of a data class
 */
class IrohaCommandTest {

    /**
     * @given AddPeer command in protobuf format
     * @when [sidechain.SideChainEvent.IrohaEvent.AddPeer] command is constructed from protobuf
     * @then [sidechain.SideChainEvent.IrohaEvent.AddPeer] is correctly constructed
     */
    @Test
    fun addPeerProtoTest() {
        val cmdBuilder = iroha.protocol.Commands.AddPeer.newBuilder()
        val address = "127.0.0.1"
        val key = ByteString.copyFromUtf8("key")
        val protoCmd = cmdBuilder
            .setPeer(
                cmdBuilder.peerBuilder
                    .setAddress(address)
                    .setPeerKey(key)
                    .build()
            )
            .build()
        val cmd = sidechain.SideChainEvent.IrohaEvent.AddPeer.fromProto(protoCmd)

        assertEquals(address, cmd.address)
        assertEquals(key, cmd.key)
    }

}
