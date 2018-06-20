package withdrawalservice

import io.reactivex.Observable


/**
 * Implementation of Withdrawal Service
 */
class WithdrawalServiceImpl(
    private val irohaHandler: Observable<WithdrawalServiceInputEvent>
) : WithdrawalService {

    /**
     * Handle IrohaEvent
     */
    override fun onIrohaEvent(ethInputEvent: WithdrawalServiceInputEvent.IrohaInputEvent): WithdrawalServiceOutputEvent {
        return WithdrawalServiceOutputEvent.EthRefund()
    }

    /**
     * Relay events to consumer
     */
    override fun output(): Observable<WithdrawalServiceOutputEvent> {
        return irohaHandler
            .map {
                when (it) {
                    is WithdrawalServiceInputEvent.IrohaInputEvent -> onIrohaEvent(it)
                }
            }
    }

}
