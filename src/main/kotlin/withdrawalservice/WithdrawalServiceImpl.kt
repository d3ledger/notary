package withdrawalservice

import config.ConfigKeys
import io.reactivex.Observable
import notary.CONFIG
import org.web3j.utils.Numeric.hexStringToByteArray
import sidechain.SideChainEvent
import sidechain.iroha.util.ModelUtil
import util.eth.hashToWithdraw
import util.eth.signUserData
import util.iroha.getRelays
import java.math.BigInteger

/**
 * Approval to be passed to the Ethereum for refund
 */
data class RollbackApproval(
        val tokenContractAddress: String,
        val amount: BigInteger,
        val account: String,
        val iroha_hash: String,
        val r: ArrayList<ByteArray>,
        val s: ArrayList<ByteArray>,
        val v: ArrayList<BigInteger>,
        val relay: String)


/**
 * Implementation of Withdrawal Service
 */
class WithdrawalServiceImpl(
        private val irohaHandler: Observable<SideChainEvent.IrohaEvent>
) : WithdrawalService {
    val notaryPeerListProvider = NotaryPeerListProviderImpl()
    val coins: HashMap<String, String> = hashMapOf("ether#ethereum" to "0x0000000000000000000000000000000000000000")
    val keypair =
            ModelUtil.loadKeypair(CONFIG[ConfigKeys.testPubkeyPath], CONFIG[ConfigKeys.testPrivkeyPath]).get()
    val masterAccount = CONFIG[ConfigKeys.notaryIrohaAccount]

    fun findInAccDetail(acc: String, name: String): String {
        val myMap = getRelays(acc, keypair)
        for (record in myMap) {
            if (record.value == name) {
                return record.key
            }
        }
        return ""
    }

    /**
     * Query all notaries for approval of refund
     */
    private fun requestNotary(event: SideChainEvent.IrohaEvent.SideChainTransfer): RollbackApproval? {
        // TODO query each notary service and if majority is achieved, send tx to Ethereum SC
        val hash = event.hash
        val amount = event.amount
        if (!coins.containsKey(event.asset)) {
            return null
        }
        val coin = coins[event.asset]
        val address = event.description
        // description field holds target account address
        val relay_address = findInAccDetail(masterAccount, event.srcAccount)
        if (relay_address == "") {
            return null
        }
        println("relay found: $relay_address")

        val vv = ArrayList<BigInteger>()
        val rr = ArrayList<ByteArray>()
        val ss = ArrayList<ByteArray>()

        notaryPeerListProvider.getPeerList().forEach { peer ->
            // TODO: replace with valid peer requests
            val signature = signUserData(hashToWithdraw(coin!!, amount, address, hash))
            val r = hexStringToByteArray(signature.substring(2, 66))
            val s = hexStringToByteArray(signature.substring(66, 130))
            val v = signature.substring(130, 132).toBigInteger(16)

            vv.add(v)
            rr.add(r)
            ss.add(s)
        }

        return RollbackApproval(coin!!, amount, address, hash, rr, ss, vv, relay_address)
    }


    /**
     * Handle IrohaEvent
     */
    override fun onIrohaEvent(irohaEvent: SideChainEvent.IrohaEvent): WithdrawalServiceOutputEvent {
        // TODO: use result or exceptions instead of nulls
        var proof: RollbackApproval? = null
        when (irohaEvent) {
            is SideChainEvent.IrohaEvent.SideChainTransfer -> {
                if (irohaEvent.dstAccount == CONFIG[ConfigKeys.notaryIrohaAccount]) {
                    proof = requestNotary(irohaEvent)
                }
            }
            else -> {}
        }
        return WithdrawalServiceOutputEvent.EthRefund(proof)
    }

    /**
     * Relay events to consumer
     */
    override fun output(): Observable<WithdrawalServiceOutputEvent> {
        return irohaHandler
                .map { onIrohaEvent(it) }
    }

}
