package notary

/**
 * Class represents [Notary] intention to [sideChain.iroha.consumer.IrohaConsumer] to add batch transaction
 * @param transactions - transactions in the batch
 */
class IrohaOrderedBatch(val transactions: List<IrohaTransaction>)
