package notary

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import config.IrohaConfig
import io.reactivex.Observable
import notary.eth.EthNotaryConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import sidechain.SideChainEvent
import java.math.BigInteger

/**
 * Test business logic of Notary.
 */
class NotaryTest {

    /** Configuration for Iroha */
    private val irohaConfig = mock<IrohaConfig>() {
        on { creator } doReturn "iroha_creator"
        on { pubkeyPath } doReturn "deploy/iroha/keys/admin@notary.pub"
        on { privkeyPath } doReturn "deploy/iroha/keys/admin@notary.priv"
        on { port } doReturn 8080
        on { hostname } doReturn "localhost"
    }

    /** Configuration for notary */
    private val notaryConfig = mock<EthNotaryConfig>() {
        on { iroha } doReturn irohaConfig
        on { notaryListStorageAccount } doReturn "notary_storage"
        on { notaryListSetterAccount } doReturn "notary_setter"
    }

    /**
     * Check transactions in ordered batch emitted on deposit event.
     * @param expectedAmount amount of assets to deposit
     * @param expectedAssetId asset id
     * @param expectedCreatorId creator of transactions
     * @param expectedHash - hash of ethereum transaction
     * @param expectedUserId - destination wallet address
     */
    private fun checkEthereumDepositResult(
        expectedAmount: String,
        expectedAssetId: String,
        expectedCreatorId: String,
        expectedHash: String,
        expectedUserId: String,
        expectedFrom: String,
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
                        assertEquals(2, commands.size)
                        cmd = commands[0]
                        if (cmd is IrohaCommand.CommandAddAssetQuantity) {
                            assertEquals(expectedAmount, cmd.amount)
                            assertEquals("${expectedAssetId}#ethereum", cmd.assetId)
                        } else {
                            fail { "Wrong IrohaCommand type" }
                        }
                        cmd = commands[1]
                        if (cmd is IrohaCommand.CommandTransferAsset) {
                            assertEquals(expectedCreatorId, cmd.srcAccountId)
                            assertEquals(expectedUserId, cmd.destAccountId)
                            assertEquals("${expectedAssetId}#ethereum", cmd.assetId)
                            assertEquals(expectedFrom, cmd.description)
                            assertEquals(expectedAmount, cmd.amount)
                        } else {
                            fail { "Wrong IrohaCommand type" }
                        }
                    }

                    else -> fail { "Wrong IrohaOrderedBatch type" }
                }
            },
            // on error
            { fail { "On error called: $it" } }
        )
    }

    /**
     * @given a custodian has 100 Wei with intention to deposit 100 Wei to Notary
     * @when a custodian transfer 100 Wei to a specified wallet and specifies Iroha wallet to deposit assets
     * @then an IrohaOrderedBatch is emitted with 2 transactions:
     * 1 - SetAccountDetail with hash
     * 2 - AddAssetQuantity with 100 Wei and TransferAsset with 100 Wei to specified account id
     */
    @Test
    fun depositEthereumTest() {
        val expectedAmount = "100"
        val expectedAssetId = "ether"
        val expectedCreatorId = "iroha_creator"
        val expectedHash = "hash"
        val expectedUserId = "from"
        val expectedFrom = "eth_from"

        val custodianIntention = mock<SideChainEvent.PrimaryBlockChainEvent.OnPrimaryChainDeposit>() {
            on { hash } doReturn expectedHash
            on { user } doReturn expectedUserId
            on { amount } doReturn expectedAmount
            on { from } doReturn expectedFrom
            on { asset } doReturn expectedAssetId
        }

        // source of events from side chains
        val obsEth = Observable.just<SideChainEvent.PrimaryBlockChainEvent>(custodianIntention)
        val notary = createEthNotary(notaryConfig, obsEth)
        val res = notary.irohaOutput()
        checkEthereumDepositResult(
            expectedAmount,
            expectedAssetId,
            expectedCreatorId,
            expectedHash,
            expectedUserId,
            expectedFrom,
            res
        )
    }

    /**
     * @given a custodian has 100 "XOR" ERC20 tokens with intention to deposit 100 "XOR" tokens to Notary
     * @when a custodian transfer 100 "XOR" tokens to a specified wallet and specifies Iroha wallet to deposit assets
     * @then an IrohaOrderedBatch is emitted with 2 transactions:
     * 1 - SetAccountDetail with hash
     * 2 - AddAssetQuantity with 100 "XOR" and TransferAsset with 100 "XOR" to specified account id
     */
    @Test
    fun depositEthereumTokenTest() {
        val expectedAmount = "100"
        val expectedAssetId = "xor"
        val expectedCreatorId = "iroha_creator"
        val expectedHash = "hash"
        val expectedUserId = "from"
        val expectedFrom = "eth_from"

        val custodianIntention = mock<SideChainEvent.PrimaryBlockChainEvent.OnPrimaryChainDeposit>() {
            on { hash } doReturn expectedHash
            on { user } doReturn expectedUserId
            on { asset } doReturn expectedAssetId
            on { amount } doReturn expectedAmount
            on { from } doReturn expectedFrom
        }

        // source of events from side chains
        val obsEth = Observable.just<SideChainEvent.PrimaryBlockChainEvent>(custodianIntention)
        val notary = createEthNotary(notaryConfig, obsEth)
        val res = notary.irohaOutput()
        checkEthereumDepositResult(
            expectedAmount,
            expectedAssetId,
            expectedCreatorId,
            expectedHash,
            expectedUserId,
            expectedFrom,
            res
        )
    }
}
