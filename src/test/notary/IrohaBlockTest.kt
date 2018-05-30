package notary


import com.google.protobuf.ByteString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File


class IrohaBlockTest {
    @Test
    fun parseProtoBlock() {
        val file = File("resources/genesis.bin")
        val bs = file.readBytes()
        val bl = iroha.protocol.BlockOuterClass.Block.parseFrom(bs)
        val txs = bl.payload.transactionsList
        val cmds = txs.flatMap { it.payload.commandsList }

        assertEquals(1, txs.size)
        assertEquals(10, cmds.size)
    }

}

class IrohaTransactionTest {
    @Test
    fun fromProtoTest() {
        val accId = "user"
        val role = "admin"
        val address = "127.0.0.1"
        val createdTime = 123L
        val quorum = 1
        val key = ByteString.copyFromUtf8("key")

        val protoCmds = listOf(
                iroha.protocol.Commands.Command.newBuilder().addPeerBuilder
                        .setPeer(
                                iroha.protocol.Primitive.Peer.newBuilder()
                                        .setAddress(address)
                                        .setPeerKey(key)
                                        .build()
                        )
                        .build(),
                iroha.protocol.Commands.Command.newBuilder().appendRoleBuilder
                        .setAccountId(accId)
                        .setRoleName(role)
                        .build()
        )

        val payload_builder = iroha.protocol.BlockOuterClass.Transaction.Payload.newBuilder()
        protoCmds.map { payload_builder.addCommands(iroha.protocol.Commands.Command.parseFrom(it.toByteArray())) }
        payload_builder
                .setCreatedTime(createdTime)
                .setCreatorAccountId(accId)
                

        val tx = iroha.protocol.BlockOuterClass.Transaction.newBuilder()


    }
}