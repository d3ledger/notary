package notary

/**
 * Class represents [Notary] intention to [sideChain.iroha.consumer.IrohaConsumer] to add batch transaction
 */
class IrohaOrderedBatch(vararg txs: IrohaTransaction) {

    /** Batch is ordered container of transaction */
    val transactions: Array<out IrohaTransaction>

    init {
        transactions = txs
    }
}
