package notary


import com.google.protobuf.ByteString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import iroha.protocol.*

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
