package notary

/**
 * Class represents [Notary] intention to [sideChain.iroha.IrohaConsumer]
 */
class IrohaOrderedBatch {

    /** Batch is ordered container of transaction, each can contain several [IrohaCommand] */
    var transactions = ArrayList<Array<IrohaCommand>>()

    /** Add transaction to the batch */
    fun addTransaction(transaction: Array<IrohaCommand>) {
        transactions.add(transaction)
    }
}