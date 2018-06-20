package withdrawalservice

/**
 * Withdrawal service is responsible for the withdrawal and rollback proof forming and interconnection with Ethereum.
 * It observers Iroha for following events:
 * 1 - transfer to master account - initiates withdrawal case
 * 2 - absence of transfer to user in deposit batch - initiates rollback case
 */
interface WithdrawalService {

    /**
     * Handle Iroha events.
     */
    fun onIrohaEvent(ethInputEvent: WithdrawalServiceInputEvent.IrohaInputEvent): WithdrawalServiceOutputEvent

    /**
     * Events emitted by [WithdrawalService]
     */
    fun output(): io.reactivex.Observable<WithdrawalServiceOutputEvent>

}
