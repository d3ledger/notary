package notary

/**
 * Class represents [Notary] intention to [sidechain.iroha.consumer.IrohaConsumer] to add batch transaction
 * @param transactions - transactions in the batch
 */
data class IrohaAtomicBatch(val transactions: List<IrohaTransaction>)
