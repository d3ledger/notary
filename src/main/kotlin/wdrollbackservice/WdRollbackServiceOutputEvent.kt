package wdrollbackservice

/**
 * Events representing final status of a withdraw attempt
 */
data class WdRollbackServiceOutputEvent(
    val irohaTxHash: String,
    val isRollbackRequired: Boolean
)
