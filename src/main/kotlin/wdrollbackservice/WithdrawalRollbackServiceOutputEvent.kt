package wdrollbackservice

/**
 * Events representing final status of a withdraw attempt
 */
data class WithdrawalRollbackServiceOutputEvent(
    val irohaTxHash: String,
    val isRollbackRequired: Boolean
)
