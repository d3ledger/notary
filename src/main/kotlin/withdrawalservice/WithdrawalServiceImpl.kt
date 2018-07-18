package withdrawalservice

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import io.reactivex.Observable
import jp.co.soramitsu.iroha.Keypair
import mu.KLogging
import notary.EthTokensProvider
import notary.EthTokensProviderImpl
import org.web3j.utils.Numeric.hexStringToByteArray
import sidechain.SideChainEvent
import sidechain.eth.util.hashToWithdraw
import sidechain.eth.util.signUserData
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.getRelays
import java.math.BigInteger

/**
 * Approval to be passed to the Ethereum for refund
 * @param tokenContractAddress Ethereum address of ERC-20 token (or 0x0000000000000000000000000000000000000000 for ether)
 * @param amount amount of token/ether to transfer
 * @param account target account
 * @param irohaHash hash of approving TransferAsset transaction in Iroha
 * @param r array of r-components of notary signatures
 * @param s array of s-components of notary signatures
 * @param v array of v-components of notary signatures
 * @param relay Ethereum address of user relay contract
 */
data class RollbackApproval(
    val tokenContractAddress: String,
    val amount: BigInteger,
    val account: String,
    val irohaHash: String,
    val r: ArrayList<ByteArray>,
    val s: ArrayList<ByteArray>,
    val v: ArrayList<BigInteger>,
    val relay: String
)


/**
 * Implementation of Withdrawal Service
 */
class WithdrawalServiceImpl(
    val withdrawalServiceConfig: WithdrawalServiceConfig,
    val keypair: Keypair,
    val irohaNetwork: IrohaNetwork,
    private val irohaHandler: Observable<SideChainEvent.IrohaEvent>
) : WithdrawalService {
    private val notaryPeerListProvider = NotaryPeerListProviderImpl()
    private val tokensProvider: EthTokensProvider = EthTokensProviderImpl(withdrawalServiceConfig.db)
    private val masterAccount = withdrawalServiceConfig.notaryIrohaAccount

    private fun findInAccDetail(acc: String, name: String): String {
        val relays = getRelays(
            withdrawalServiceConfig.iroha,
            keypair,
            irohaNetwork,
            acc,
            withdrawalServiceConfig.registrationIrohaAccount
        )
        for (record in relays.get()) {
            if (record.value == name) {
                return record.key
            }
        }
        return ""
    }

    /**
     * Query all notaries for approval of refund
     */
    private fun requestNotary(event: SideChainEvent.IrohaEvent.SideChainTransfer): Result<RollbackApproval, Exception> {
        return Result.of {
            val hash = event.hash
            val amount = event.amount
            val coins = tokensProvider.getTokens().get().toMutableMap()
            coins["0x0000000000000000000000000000000000000000"] = "ether"

            if (!event.asset.contains("#ethereum")) {
                throw Exception("Incorrect asset name in Iroha event: " + event.asset)
            }
            val asset = event.asset.replace("#ethereum", "")

            var coinAddress = ""
            for (coin in coins) {
                if (coin.value == asset) {
                    coinAddress = coin.key
                    break
                }
            }
            if (coinAddress == "") {
                throw Exception("Not supported token type")
            }

            val address = event.description
            // description field holds target account address
            val relayAddress = findInAccDetail(masterAccount, event.srcAccount)
            if (relayAddress == "") {
                throw Exception("Unable to find relay for " + event.srcAccount)
            }
            logger.info { "relay found: $relayAddress" }

            val vv = ArrayList<BigInteger>()
            val rr = ArrayList<ByteArray>()
            val ss = ArrayList<ByteArray>()

            notaryPeerListProvider.getPeerList().forEach { peer ->
                // TODO: replace with valid peer requests
                val signature =
                    signUserData(withdrawalServiceConfig.ethereum, hashToWithdraw(coinAddress, amount, address, hash))
                val r = hexStringToByteArray(signature.substring(2, 66))
                val s = hexStringToByteArray(signature.substring(66, 130))
                val v = signature.substring(130, 132).toBigInteger(16)

                vv.add(v)
                rr.add(r)
                ss.add(s)
            }
            RollbackApproval(coinAddress, amount, address, hash, rr, ss, vv, relayAddress)
        }
    }


    /**
     * Handle IrohaEvent
     */
    override fun onIrohaEvent(irohaEvent: SideChainEvent.IrohaEvent): Result<WithdrawalServiceOutputEvent, Exception> {
        when (irohaEvent) {
            is SideChainEvent.IrohaEvent.SideChainTransfer -> {
                if (irohaEvent.dstAccount == withdrawalServiceConfig.notaryIrohaAccount) {
                    return requestNotary(irohaEvent)
                        .map { WithdrawalServiceOutputEvent.EthRefund(it) }
                }
            }
            else -> {
            }
        }
        return Result.error(Exception("Wrong event type or wrong destination account"))
    }

    /**
     * Relay events to consumer
     */
    override fun output(): Observable<Result<WithdrawalServiceOutputEvent, Exception>> {
        return irohaHandler
            .map {
                onIrohaEvent(it)
            }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
