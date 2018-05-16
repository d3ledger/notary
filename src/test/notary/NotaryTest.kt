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
     * @then an IrohaOrderedBatch is emitted with 3 transactions:
     * 1 - SetAccountDetail with hash
     * 2 - AddAssetQuantity with 100 Wei
     * 3 - TransferAsset with 100 Wei to specified asset
     */
    @Test
    fun depositEthereumTest() {
        val amount = BigInteger.valueOf(100)
        val accountId = "user@test"
        val assetId = CONFIG[ConfigKeys.irohaEthToken]
        val creatorId = CONFIG[ConfigKeys.irohaCreator]
        val txHash = "hash"
        val txFrom = "from"

        val custodianIntention = mock<NotaryInputEvent.EthChainInputEvent.OnEthSidechainDeposit>() {
            on { hash } doReturn txHash
            on { from } doReturn txFrom
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
                    assertEquals(3, txs.size)

                    val setAccountDetail = txs[0].commands
                    assertEquals(1, setAccountDetail.size)
                    var cmd = setAccountDetail.first()
                    if (cmd is IrohaCommand.CommandSetAccountDetail) {
                        assertEquals(creatorId, cmd.accountId)
                        assertEquals(txHash, cmd.key)
                        assertEquals(txFrom, cmd.value)
                    } else {
                        fail { "Wrong IrohaCommand type" }
                    }

                    val addAssetQuantity = txs[1].commands
                    assertEquals(1, addAssetQuantity.size)
                    cmd = addAssetQuantity.first()
                    if (cmd is IrohaCommand.CommandAddAssetQuantity) {
                        assertEquals(amount.toString(), cmd.amount)
                        assertEquals(creatorId, cmd.accountId)
                        assertEquals(assetId, cmd.assetId)
                    } else {
                        fail { "Wrong IrohaCommand type" }
                    }

                    val transferAsset = txs[2].commands
                    assertEquals(1, transferAsset.size)
                    cmd = transferAsset.first()
                    if (cmd is IrohaCommand.CommandTransferAsset) {
                        assertEquals(creatorId, cmd.srcAccountId)
                        assertEquals(accountId, cmd.destAccountId)
                        assertEquals(assetId, cmd.assetId)
                        assertEquals(txHash, cmd.description)
                        assertEquals(amount.toString(), cmd.amount)
                    } else {
                        fail { "Wrong IrohaCommand type" }
                    }
                }
            }
        }
    }

    /**
     * @given a custodian has 50 Wei in Notary and has intention to withdraw them
     * @when a custodian sends request
     * @then an IrohaOrderedBatch is emmited with exactly one transaction with exactly one command
     */
    @Test
    fun withdrawEthereumTest() {
    }

}
