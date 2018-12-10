package notary

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import config.IrohaConfig
import config.IrohaCredentialConfig
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import model.IrohaCredential
import notary.eth.EthNotaryConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import provider.NotaryPeerListProvider
import sidechain.SideChainEvent
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.ModelUtil
import java.math.BigInteger
import kotlin.test.assertEquals

/**
 * Test business logic of Notary.
 */
class NotaryTest {

    /** Configuration for Iroha */
    private val irohaConfig = mock<IrohaConfig>() {
        on { port } doReturn 8080
        on { hostname } doReturn "localhost"
    }

    private val credentialConfig = mock<IrohaCredentialConfig>() {
        on { privkeyPath } doReturn "deploy/iroha/keys/test@notary.priv"
        on { pubkeyPath } doReturn "deploy/iroha/keys/test@notary.pub"
        on { accountId } doReturn "creator@iroha"
    }

    private val irohaCredential = IrohaCredential("creator@iroha", ModelUtil.generateKeypair())
    private val irohaNetwork = mock<IrohaNetwork>()

    /** Configuration for notary */
    private val notaryConfig = mock<EthNotaryConfig>() {
        on { iroha } doReturn irohaConfig
        on { notaryListStorageAccount } doReturn "listener@notary"
        on { notaryListSetterAccount } doReturn "setter@notary"
        on { notaryCredential } doReturn credentialConfig
    }

    private val peerListProvider = mock<NotaryPeerListProvider>()

    init {
        try {
            System.loadLibrary("irohajava")
        } catch (e: UnsatisfiedLinkError) {
            System.err.println("Native code library failed to load. \n$e")
            System.exit(1)
        }
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
        expectedTime: BigInteger,
        result: Observable<IrohaOrderedBatch>
    ) {
        val observer = TestObserver<IrohaOrderedBatch>()
        result.subscribe(observer)

        observer.assertNoErrors()
        observer.assertComplete()
        observer.assertValueCount(1)
        observer.assertValue {
            val txs = it.transactions
            assertEquals(2, txs.size)

            var commands = txs[0].commands
            assertEquals(expectedTime, txs[0].createdTime)
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
            assertEquals(expectedTime, txs[1].createdTime)
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

            true
        }
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
        val expectedCreatorId = "creator@iroha"
        val expectedHash = "hash"
        val expectedUserId = "from"
        val expectedFrom = "eth_from"
        val expectedTime = BigInteger.TEN

        val custodianIntention = SideChainEvent.PrimaryBlockChainEvent.OnPrimaryChainDeposit(
            expectedHash,
            expectedTime,
            expectedUserId,
            expectedAssetId,
            expectedAmount,
            expectedFrom
        )

        // source of events from side chains
        val obsEth = Observable.just<SideChainEvent.PrimaryBlockChainEvent>(custodianIntention)
        val notary = createEthNotary(irohaCredential, irohaNetwork, obsEth, peerListProvider)
        val res = notary.irohaOutput()
        checkEthereumDepositResult(
            expectedAmount,
            expectedAssetId,
            expectedCreatorId,
            expectedHash,
            expectedUserId,
            expectedFrom,
            expectedTime,
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
        val expectedCreatorId = "creator@iroha"
        val expectedHash = "hash"
        val expectedUserId = "from"
        val expectedFrom = "eth_from"
        val expectedTime = BigInteger.TEN
        val custodianIntention = SideChainEvent.PrimaryBlockChainEvent.OnPrimaryChainDeposit(
            expectedHash,
            expectedTime,
            expectedUserId,
            expectedAssetId,
            expectedAmount,
            expectedFrom
        )

        // source of events from side chains
        val obsEth = Observable.just<SideChainEvent.PrimaryBlockChainEvent>(custodianIntention)
        val notary = createEthNotary(irohaCredential, irohaNetwork, obsEth, peerListProvider)
        val res = notary.irohaOutput()
        checkEthereumDepositResult(
            expectedAmount,
            expectedAssetId,
            expectedCreatorId,
            expectedHash,
            expectedUserId,
            expectedFrom,
            expectedTime,
            res
        )
    }
}
