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
            val cmds = tx.payload.commandsList
                .filter { it.hasAddPeer() }
                .map {
                    if (it.hasAddPeer())
                        IrohaCommand.CommandAddPeer.fromProto(it.toByteArray())
                    if (it.hasSetAccountDetail())
                        IrohaCommand.CommandSetAccountDetail.fromProto(it.toByteArray())
                } as List<IrohaCommand>

            val creator = tx.payload.creatorAccountId
            return IrohaTransaction(creator, cmds)
        }
    }
}
