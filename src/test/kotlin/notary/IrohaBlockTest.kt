package notary


import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import sidechain.iroha.IrohaBlockStub
import java.io.File

class IrohaBlockTest {
    @Test
    fun parseProtoBlock() {
        val file = File("resources/genesis.bin")
        val bs = file.readBytes()
        val bl = iroha.protocol.BlockOuterClass.Block.parseFrom(bs)
        val block = IrohaBlockStub.fromProto(bs)
        val txs = bl.payload.transactionsList
        val cmds = txs.flatMap { it.payload.commandsList }

        val hash = ByteArray(32)
        hash.fill(0)

        val command = block.transactions.first().commands.first() as IrohaCommand.CommandAddPeer
        assertEquals(1, txs.size)
        assertEquals(10, cmds.size)
        assertEquals(1, block.txNumber)
        assertEquals(1, block.height)
        assertEquals("localhost:10001", command.address)
        assertArrayEquals(hash, block.prevBlockHash)

    }

}
