package notary

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import main.CONFIG
import main.ConfigKeys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.math.BigInteger

/**
 * Test business logic of Notary.
 */
class NotaryTest {

    /**
     * @given a custodian has 100 Wei with intention to deposit 100 Wei to Notary
     * @when a custodian transfer 100 Wei to a specified wallet and specifies Iroha wallet to deposit assets
     * @then an IrohaOrderedBatch is emitted with exactly one transaction with exactly one command AddAssetQuantity
     * where 100 Wei are added to specified Iroha address
     */
    @Test
    fun depositEthereumTest() {
        val amount = BigInteger.valueOf(100)
        val accountId = "user@test"
        val assetId = CONFIG[ConfigKeys.irohaEthToken]

        val custodianIntention = mock<NotaryInputEvent.EthChainInputEvent.OnEthSidechainDeposit>() {
            on { hash } doReturn "hash"
            on { from } doReturn "from"
            on { value } doReturn amount
            on { input } doReturn accountId
        }

        // source of events from side chains
        val obsEth = Observable.just<NotaryInputEvent>(custodianIntention)
        val obsIroha = Observable.empty<NotaryInputEvent>()

        val notary = NotaryImpl(obsEth, obsIroha)
        val res = notary.irohaOutput()

        res.subscribe() {
            when (it) {
                is IrohaOrderedBatch -> {
                    val txs = it.transactions
                    assertEquals(1, txs.size)

                    val cmds = txs.first().commands
                    assertEquals(1, cmds.size)

                    val cmd = cmds.first()
                    if (cmd is IrohaCommand.CommandAddAssetQuantity) {
                        assertEquals(amount.toString(), cmd.amount)
                        assertEquals(accountId, cmd.accountId)
                        assertEquals(assetId, cmd.assetId)
                    } else {
                        fail { "Wrong IrohaCommand type" }
                    }
                }
            }
        }
    }

}
