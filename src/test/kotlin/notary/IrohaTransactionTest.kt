package notary

import com.google.protobuf.ByteString
import iroha.protocol.Commands
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class IrohaTransactionTest {
    @Test
    fun fromProtoTest() {
        val accId = "user"
        val asset = "coin"
        val domain = "ru"
        val address = "127.0.0.1"
        val precision: Short = 4
        val createdTime = 123L
        val quorum = 1
        val key = ByteString.copyFromUtf8("key")

        val protoCmds = listOf(
            Commands.Command.newBuilder().createAssetBuilder
                .setAssetName(asset)
                .setDomainId(domain)
                .setPrecision(precision.toInt())
                .build(),
            Commands.Command.newBuilder().addPeerBuilder
                .setPeer(
                    iroha.protocol.Primitive.Peer.newBuilder()
                        .setAddress(address)
                        .setPeerKey(key)
                        .build()
                )
                .build()
        )

        val payloadBuilder = iroha.protocol.BlockOuterClass.Transaction.Payload.newBuilder()
        val payload = payloadBuilder
            .setReducedPayload(
                iroha.protocol.BlockOuterClass.Transaction.Payload.ReducedPayload.newBuilder()
                    .addCommands(Commands.Command.newBuilder().setCreateAsset(protoCmds[0] as Commands.CreateAsset))
                    .addCommands(Commands.Command.newBuilder().setAddPeer(protoCmds[1] as Commands.AddPeer))
                    .setCreatedTime(createdTime)
                    .setCreatorAccountId(accId)
                    .setQuorum(quorum)
                    .build()
            )
            .build()


        val protoTx = iroha.protocol.BlockOuterClass.Transaction.newBuilder()
            .setPayload(payload)
            .build()

        val tx = IrohaTransaction.fromProto(protoTx.toByteArray())

        Assertions.assertEquals(accId, tx.creator)
        Assertions.assertEquals(1, tx.commands.size)
        Assertions.assertEquals(IrohaCommand.CommandAddPeer(address, key.toByteArray()), tx.commands.first())
    }
}
