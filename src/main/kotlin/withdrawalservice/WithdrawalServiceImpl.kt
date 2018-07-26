package withdrawalservice

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.EthereumPasswords
import io.reactivex.Observable
import jp.co.soramitsu.iroha.Keypair
import mu.KLogging
import notary.EthTokensProvider
import notary.EthTokensProviderImpl
import notary.endpoint.eth.AmountType
import org.web3j.utils.Numeric.hexStringToByteArray
import sidechain.SideChainEvent
import sidechain.eth.util.hashToWithdraw
import sidechain.eth.util.signUserData
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.getRelays
import java.math.BigDecimal
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
    val amount: AmountType,
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
    val withdrawalServicePasswords: EthereumPasswords,
    val keypair: Keypair,
    val irohaNetwork: IrohaNetwork,
    private val irohaHandler: Observable<SideChainEvent.IrohaEvent>
) : WithdrawalService {
    private val notaryPeerListProvider = NotaryPeerListProviderImpl()
    private val tokensProvider: EthTokensProvider = EthTokensProviderImpl(withdrawalServiceConfig.db)
    private val masterAccount = withdrawalServiceConfig.notaryIrohaAccount

    private fun findInAccDetail(acc: String, name: String): Result<String, Exception> {
        return getRelays(
            withdrawalServiceConfig.iroha,
            keypair,
            irohaNetwork,
            acc,
            withdrawalServiceConfig.registrationIrohaAccount
        ).map { relays ->
            val keys = relays.filterValues { it == name }.keys
            if (keys.isEmpty())
                throw Exception("No relay address in account details $acc set by $name")
            else
                keys.first()
        }
    }

    /**
     * Query all notaries for approval of refund
     * @param event - iroha transfer event
     * @return rollback approval or exception
     */
    private fun requestNotary(event: SideChainEvent.IrohaEvent.SideChainTransfer): Result<RollbackApproval, Exception> {
        // description field holds target account address
        return findInAccDetail(masterAccount, event.srcAccount)
            .map { relayAddress ->
                val hash = event.hash
                var amount = event.amount
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

                if (asset == "ether") {
                    amount = BigDecimal(amount).multiply(BigDecimal.TEN.pow(18)).toBigInteger().toString()
                }

                val address = event.description
                val vv = ArrayList<BigInteger>()
                val rr = ArrayList<ByteArray>()
                val ss = ArrayList<ByteArray>()

                notaryPeerListProvider.getPeerList().forEach { peer ->
                    // TODO: replace with valid peer requests
                    val signature =
                        signUserData(
                            withdrawalServiceConfig.ethereum,
                            withdrawalServicePasswords,
                            hashToWithdraw(coinAddress, amount, address, hash)
                        )
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
     * @param irohaEvent - iroha event
     * @return withdrawal service output event or exception
     */
    override fun onIrohaEvent(irohaEvent: SideChainEvent.IrohaEvent): Result<WithdrawalServiceOutputEvent, Exception> {
        when (irohaEvent) {
            is SideChainEvent.IrohaEvent.SideChainTransfer -> {
                if (irohaEvent.dstAccount == withdrawalServiceConfig.notaryIrohaAccount) {
                    logger.info { "Withdrawal event" }
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
