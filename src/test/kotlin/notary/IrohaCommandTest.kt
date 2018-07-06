package notary

import com.google.protobuf.ByteString
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Test accessor of a data class
 */
class IrohaCommandTest {

    /** Test an ability to mock a final class */
    @Test
    fun testCommandAddAssetQuantity() {
        val expected = "I can mock final classes"
        val m = mock<IrohaCommand.CommandAddAssetQuantity>() {
            on { accountId } doReturn expected
        }

        assertEquals(expected, m.accountId)
    }

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
        val cmd = sidechain.SideChainEvent.IrohaEvent.OnIrohaAddPeer.fromProto(protoCmd.toByteArray())

        assertEquals(address, cmd.address)
        assertEquals(key, ByteString.copyFrom(cmd.peerKey))

    }

}
