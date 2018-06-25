package withdrawalservice

import io.reactivex.Observable
import sidechain.SideChainEvent


/**
 * Implementation of Withdrawal Service
 */
class WithdrawalServiceImpl(
    private val irohaHandler: Observable<SideChainEvent.IrohaEvent>
) : WithdrawalService {
    val notaryPeerListProvider = NotaryPeerListProviderImpl()

    /**
     * Query all notaries for approval of refund
     */
    private fun requestNotary(event: SideChainEvent.IrohaEvent.OnIrohaSideChainTransfer) {
        // TODO query each notary service and if majority is achieved, send tx to Ethereum SC

        for (peer in notaryPeerListProvider.getPeerList())
        ; // TODO query notary
    }


    /**
     * Handle IrohaEvent
     */
    override fun onIrohaEvent(irohaEvent: SideChainEvent.IrohaEvent): WithdrawalServiceOutputEvent {
        // TODO add effective implementation
        // 1. ask rollback approval from notary
        // 2. form an output event with all data needed for smart contract rollback call
        when (irohaEvent) {
            is SideChainEvent.IrohaEvent.OnIrohaSideChainTransfer -> requestNotary(irohaEvent)
        }
        return WithdrawalServiceOutputEvent.EthRefund()
    }

    /**
     * Relay events to consumer
     */
    override fun output(): Observable<WithdrawalServiceOutputEvent> {
        return irohaHandler
            .map { onIrohaEvent(it) }
    }

}
