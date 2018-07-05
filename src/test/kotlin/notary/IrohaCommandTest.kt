package notary

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import iroha.protocol.BlockOuterClass
import jp.co.soramitsu.iroha.ByteVector
import jp.co.soramitsu.iroha.ModelCrypto
import jp.co.soramitsu.iroha.ModelTransactionBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sidechain.iroha.IrohaInitialization
import java.math.BigInteger
import java.util.*

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

    fun ByteVector.toByteArray(): ByteArray {
        val size = this.size().toInt()
        val bs = kotlin.ByteArray(size)
        for (i in 0 until size) {
            bs[i] = this.get(i).toByte()
        }
        return bs
    }

    @Test
    fun addPeerProtoTest() {
        IrohaInitialization.loadIrohaLibrary()


        val address = "127.0.0.1:8080"
        val keys = ModelCrypto().generateKeypair()


        val tx = ModelTransactionBuilder()
            .creatorAccountId("a@a")
            .createdTime(BigInteger.valueOf(1530753972862))
            .addPeer(address, keys.publicKey())
            .build()
            .signAndAddSignature(keys)
            .finish()
            .blob()


        val tmp = BlockOuterClass.Transaction.parseFrom(tx.blob().toByteArray())
        val f = tmp.payload.reducedPayload.commandsList.first()
        val cmd = IrohaCommand.CommandAddPeer.fromProto(f.toByteArray())

        assertEquals(address, cmd.address)
        assertTrue(Arrays.equals(keys.publicKey().blob().toByteArray(), cmd.peerKey))

    }

}
