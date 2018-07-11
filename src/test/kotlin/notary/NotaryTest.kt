package notary

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import config.ConfigKeys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import sidechain.SideChainEvent
import java.math.BigInteger

/**
 * Test business logic of Notary.
 */
class NotaryTest {

    /**
     * Iroha output observable
     */
    private val obsIroha = Observable.empty<SideChainEvent>()

    /**
     * Check transactions in ordered batch emitted on deposit event.
     * @param expectedAmount amount of assets to deposit
     * @param expectedAssetId asset id
     * @param expectedCreatorId creator of transactions
     * @param expectedHash - hash of ethereum transaction
     * @param expectedUserId - destination wallet address
     */
    private fun checkEthereumDepositResult(
        expectedAmount: BigInteger,
        expectedAssetId: String,
        expectedCreatorId: String,
        expectedHash: String,
        expectedUserId: String,
        result: Observable<IrohaOrderedBatch>
    ) {
        result.subscribe(
            {
                when (it) {
                    is IrohaOrderedBatch -> {
                        val txs = it.transactions
                        assertEquals(3, txs.size)

                        var commands = txs[0].commands
                        assertEquals(1, commands.size)
                        var cmd = commands.first()
                        if (cmd is IrohaCommand.CommandSetAccountDetail) {
                            assertEquals(expectedCreatorId, cmd.accountId)
                            assertEquals("last_tx", cmd.key)
                            assertEquals(expectedHash, cmd.value)
                        } else {
                            fail { "Wrong IrohaCommand type" }
                        }

                        commands = txs[1].commands
                        assertEquals(1, commands.size)
                        cmd = commands.first()
                        if (cmd is IrohaCommand.CommandCreateAsset) {
                            assertEquals(expectedAssetId, cmd.assetName)
                            assertEquals("ethereum", cmd.domainId)
                            assertEquals(0, cmd.precision)
                        } else {
                            fail { "Wrong IrohaCommand type" }
                        }

                        commands = txs[2].commands
                        assertEquals(2, commands.size)
                        cmd = commands[0]
                        if (cmd is IrohaCommand.CommandAddAssetQuantity) {
                            assertEquals(expectedAmount.toString(), cmd.amount)
                            assertEquals(expectedCreatorId, cmd.accountId)
                            assertEquals("${expectedAssetId}#ethereum", cmd.assetId)
                        } else {
                            fail { "Wrong IrohaCommand type" }
                        }
                        cmd = commands[1]
                        if (cmd is IrohaCommand.CommandTransferAsset) {
                            assertEquals(expectedCreatorId, cmd.srcAccountId)
                            assertEquals(expectedUserId, cmd.destAccountId)
                            assertEquals("${expectedAssetId}#ethereum", cmd.assetId)
                            assertEquals("", cmd.description)
                            assertEquals(expectedAmount.toString(), cmd.amount)
                        } else {
                            fail { "Wrong IrohaCommand type" }
                        }
                    }

                    else -> fail { "Wrong IrohaOrderedBatch type" }
                }
            },
            // on error
            { fail { "On error called" } }
        )
    }

    /**
     * @given a custodian has 100 Wei with intention to deposit 100 Wei to Notary
     * @when a custodian transfer 100 Wei to a specified wallet and specifies Iroha wallet to deposit assets
     * @then an IrohaOrderedBatch is emitted with 3 transactions:
     * 1 - SetAccountDetail with hash
     * 2 - CreateAsset with "ether" asset name
     * 3 - AddAssetQuantity with 100 Wei and TransferAsset with 100 Wei to specified account id
     */
    @Test
    fun depositEthereumTest() {
        val expectedAmount = BigInteger.valueOf(100)
        val expectedAssetId = "ether"
        val expectedCreatorId = CONFIG[ConfigKeys.notaryIrohaAccount]
        val expectedHash = "hash"
        val expectedUserId = "from"

        val custodianIntention = mock<SideChainEvent.EthereumEvent.OnEthSidechainDeposit>() {
            on { hash } doReturn expectedHash
            on { user } doReturn expectedUserId
            on { amount } doReturn expectedAmount
        }

        // source of events from side chains
        val obsEth = Observable.just<SideChainEvent>(custodianIntention)

        val notary = NotaryImpl(obsEth, obsIroha)
        val res = notary.irohaOutput()
        checkEthereumDepositResult(
            expectedAmount,
            expectedAssetId,
            expectedCreatorId,
            expectedHash,
            expectedUserId,
            res
        )
    }

    /**
     * @given a custodian has 100 "XOR" ERC20 tokens with intention to deposit 100 "XOR" tokens to Notary
     * @when a custodian transfer 100 "XOR" tokens to a specified wallet and specifies Iroha wallet to deposit assets
     * @then an IrohaOrderedBatch is emitted with 3 transactions:
     * 1 - SetAccountDetail with hash
     * 2 - CreateAsset with "XOR" asset name
     * 3 - AddAssetQuantity with 100 "XOR" and TransferAsset with 100 "XOR" to specified account id
     */
    @Test
    fun depositEthereumTokenTest() {
        val expectedAmount = BigInteger.valueOf(100)
        val expectedAssetId = "xor"
        val expectedCreatorId = CONFIG[ConfigKeys.notaryIrohaAccount]
        val expectedHash = "hash"
        val expectedUserId = "from"

        val custodianIntention = mock<SideChainEvent.EthereumEvent.OnEthSidechainDepositToken>() {
            on { hash } doReturn expectedHash
            on { user } doReturn expectedUserId
            on { token } doReturn expectedAssetId
            on { amount } doReturn expectedAmount
        }

        // source of events from side chains
        val obsEth = Observable.just<SideChainEvent>(custodianIntention)

        val notary = NotaryImpl(obsEth, obsIroha)
        val res = notary.irohaOutput()
        checkEthereumDepositResult(
            expectedAmount,
            expectedAssetId,
            expectedCreatorId,
            expectedHash,
            expectedUserId,
            res
        )
    }
}
