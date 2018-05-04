package notary

/**
 * Class represents [Notary] intention to [sideChain.iroha.consumer.IrohaConsumer] to add transaction
 * @param creator account id of transaction creator
 * @param cmds commands to be sent to Iroha
 */
class IrohaTransaction(val creator: String, vararg cmds: IrohaCommand) {

    /** Transaction contains several [IrohaCommand] */
    val commands: Array<out IrohaCommand>

    init {
        commands = cmds
    }
}
