package withdrawalservice

import io.reactivex.Observable
import sidechain.SideChainEvent


/**
 * Implementation of Withdrawal Service
 */
class WithdrawalServiceImpl(
    private val irohaHandler: Observable<SideChainEvent.IrohaEvent>
) : WithdrawalService {

    /**
     * Handle IrohaEvent
     */
    override fun onIrohaEvent(ethInputEvent: SideChainEvent.IrohaEvent): WithdrawalServiceOutputEvent {
        // TODO add effective implementation
        // 1. ask rollback approval from notary
        // 2. form an output event with all data needed for smart contract rollback call
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
