package withdrawalservice

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.squareup.moshi.Moshi
import io.reactivex.Observable
import jp.co.soramitsu.iroha.Keypair
import mu.KLogging
import notary.endpoint.eth.AmountType
import notary.endpoint.eth.BigIntegerMoshiAdapter
import notary.endpoint.eth.EthNotaryResponse
import notary.endpoint.eth.EthNotaryResponseMoshiAdapter
import provider.NotaryPeerListProviderImpl
import provider.eth.EthTokensProvider
import provider.eth.EthTokensProviderImpl
import sidechain.SideChainEvent
import sidechain.eth.util.extractVRS
import sidechain.eth.util.findInTokens
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.getAccountDetails
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
    val keypair: Keypair,
    val irohaNetwork: IrohaNetwork,
    private val irohaHandler: Observable<SideChainEvent.IrohaEvent>
) : WithdrawalService {
    private val notaryPeerListProvider = NotaryPeerListProviderImpl(
        withdrawalServiceConfig.iroha,
        keypair,
        withdrawalServiceConfig.notaryListStorageAccount,
        withdrawalServiceConfig.notaryListSetterAccount
    )
    private val tokensProvider: EthTokensProvider = EthTokensProviderImpl(
        withdrawalServiceConfig.iroha,
        keypair,
        withdrawalServiceConfig.notaryIrohaAccount,
        withdrawalServiceConfig.tokenStorageAccount
    )
    private val masterAccount = withdrawalServiceConfig.notaryIrohaAccount

    private fun findInAccDetail(acc: String, name: String): Result<String, Exception> {
        return getAccountDetails(
            withdrawalServiceConfig.iroha,
            keypair,
            irohaNetwork,
            acc,
            withdrawalServiceConfig.registrationIrohaAccount
        ).map { relays ->
            val keys = relays.filterValues { it == name }.keys
            if (keys.isEmpty())
                throw Exception("No relay address in account details $acc bind to $name")
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
                val amount = event.amount
                val coins = tokensProvider.getTokens().get().toMutableMap()
                if (!event.asset.contains("#ethereum")) {
                    throw Exception("Incorrect asset name in Iroha event: " + event.asset)
                }
                val asset = event.asset.replace("#ethereum", "")

                val coinAddress = findInTokens(asset, coins)

                val address = event.description
                val vv = ArrayList<BigInteger>()
                val rr = ArrayList<ByteArray>()
                val ss = ArrayList<ByteArray>()

                notaryPeerListProvider.getPeerList().forEach { peer ->
                    logger.info { "Query $peer for proof" }
                    val res: khttp.responses.Response
                    try {
                        res = khttp.get("$peer/eth/$hash")
                    } catch (e: Exception) {
                        logger.warn { "Exception was thrown while refund server request: server $peer" }
                        logger.warn { e.localizedMessage }
                        return@forEach
                    }
                    if (res.statusCode != 200) {
                        logger.warn { "Error happened while refund server request: server $peer, error ${res.statusCode}" }
                        return@forEach
                    }

                    val moshi = Moshi
                        .Builder()
                        .add(EthNotaryResponseMoshiAdapter())
                        .add(BigInteger::class.java, BigIntegerMoshiAdapter())
                        .build()!!
                    val ethNotaryAdapter = moshi.adapter(EthNotaryResponse::class.java)!!
                    val response = ethNotaryAdapter.fromJson(res.jsonObject.toString())

                    when (response) {
                        is EthNotaryResponse.Error -> {
                            logger.warn { "EthNotaryResponse.Error: ${response.reason}" }
                            return@forEach
                        }

                        is EthNotaryResponse.Successful -> {
                            val signature = response.ethSignature
                            val vrs = extractVRS(signature)
                            vv.add(vrs.v)
                            rr.add(vrs.r)
                            ss.add(vrs.s)
                        }
                    }
                }
                if (vv.size == 0) {
                    throw Exception("Not a single valid response was received from any refund server")
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
                logger.info { "Iroha transfer event to ${irohaEvent.dstAccount}" }

                if (irohaEvent.dstAccount == withdrawalServiceConfig.notaryIrohaAccount) {
                    logger.info { "Withdrawal event" }
                    return requestNotary(irohaEvent)
                        .map { WithdrawalServiceOutputEvent.EthRefund(it) }
                }
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
