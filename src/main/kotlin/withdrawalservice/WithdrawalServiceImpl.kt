package withdrawalservice

import io.reactivex.Observable
import sidechain.SideChainEvent

/**
 * Approval to be passed to the Ethereum for refund
 */
// TODO
class RollbackApproval()

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
    private fun requestNotary(event: SideChainEvent.IrohaEvent.OnIrohaSideChainTransfer): RollbackApproval {
        // TODO query each notary service and if majority is achieved, send tx to Ethereum SC

        notaryPeerListProvider.getPeerList().forEach { peer ->
            // TODO query notary
        }

        // TODO concatenate signatures
        return RollbackApproval()
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
