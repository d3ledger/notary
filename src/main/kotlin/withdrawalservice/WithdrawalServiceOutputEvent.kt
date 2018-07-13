package withdrawalservice

/**
 * Events are emitted by withdrawal service
 */
sealed class WithdrawalServiceOutputEvent {

    /**
     * Refund in Ethereum chain
     */
    data class EthRefund(val proof: RollbackApproval?) : WithdrawalServiceOutputEvent()
}
