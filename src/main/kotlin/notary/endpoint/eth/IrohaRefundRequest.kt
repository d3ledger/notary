package notary.endpoint.eth

/**
 * Interface represents custodian's request about rollback inside Iroha in case of problem of rollback to sidechain
 * @param irohaTx - Hash of initial Iroha transaction for requesting refund to sidechain
 */
data class IrohaRefundRequest(
    val irohaTx: IrohaTransactionHashType
)

