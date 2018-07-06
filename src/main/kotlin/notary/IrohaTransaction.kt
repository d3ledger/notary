package notary

/**
 * Class represents [Notary] intention to [sidechain.iroha.consumer.IrohaConsumer] to add transaction
 * @param creator account id of transaction creator
 * @param commands commands to be sent to Iroha
 */
data class IrohaTransaction(
    val creator: String,
    val commands: List<IrohaCommand>
) {
    companion object {
        /**
         * This implementation takes only AddPeer commands !!
         * TODO x3medima17, implement all commands
         *
         */
        fun fromProto(bytes: ByteArray): IrohaTransaction {
            val tx = iroha.protocol.BlockOuterClass.Transaction.parseFrom(bytes)
            val cmds = tx.payload.reducedPayload.commandsList
                .filter { it.hasAddPeer() }
                .map {
                    IrohaCommand.CommandAddPeer.fromProto(it.toByteArray())
                }

            val creator = tx.payload.reducedPayload.creatorAccountId
            return IrohaTransaction(creator, cmds)
        }
    }
}
