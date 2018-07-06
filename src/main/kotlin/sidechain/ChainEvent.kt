package sidechain

import notary.IrohaCommand
import sidechain.iroha.util.toBigInteger
import java.math.BigInteger

/**
 * All events emitted by side chains.
 */
sealed class SideChainEvent {

    /**
     * Class represents events in Iroha chain
     */
    sealed class IrohaEvent : SideChainEvent() {

        /**
         * Event which raised on adding new peer in Iroha network
         *
         * @param address peer's address, ip and port
         * @param key peer's key
         */
        data class OnIrohaAddPeer(
            val cmd: IrohaCommand.CommandAddPeer
        ) : IrohaEvent() {
            companion object {
                /**
                 * Takes a binary proto command and creates a model command AddPeer
                 * @param bytes the command represented as byte array
                 */
                fun fromProto(bytes: ByteArray): IrohaCommand.CommandAddPeer {
                    val generic = iroha.protocol.Commands.Command.parseFrom(bytes)
                    val cmd = if (generic.hasAddPeer()) {
                        generic.addPeer
                    } else iroha.protocol.Commands.AddPeer.parseFrom(bytes)

                    return IrohaCommand.CommandAddPeer(cmd.peer.address, cmd.peer.peerKey.toByteArray())
                }
            }

        }

        /**
         * Event which is raised when custodian transfer assets to notary account to withdraw asset
         *
         * @param asset is asset id in Iroha
         * @param amount of ethereum to withdraw
         */
        data class OnIrohaSideChainTransfer(
            val cmd: IrohaCommand.CommandTransferAsset
        ) : IrohaEvent() {

            companion object {
                /**
                 * Takes a binary proto command and creates a model command AddPeer
                 * @param bytes the command represented as byte array
                 */
                fun fromProto(bytes: ByteArray): IrohaCommand.CommandTransferAsset {
                    val generic = iroha.protocol.Commands.Command.parseFrom(bytes)
                    val cmd = if (generic.hasTransferAsset()) {
                        generic.transferAsset
                    } else iroha.protocol.Commands.TransferAsset.parseFrom(bytes)

                    return IrohaCommand.CommandTransferAsset(
                        cmd.srcAccountId,
                        cmd.destAccountId,
                        cmd.assetId,
                        cmd.description,
                        cmd.amount.value.toBigInteger()
                    )
                }
            }
        }
    }

    /**
     * Common class for all interested Ethereum events
     */
    sealed class EthereumEvent : SideChainEvent() {

        /**
         * Event which raised on a new transfer transaction to Ethereum wallet
         * @param hash transaction hash
         * @param user user name in Iroha
         * @param amount amount of Ether transfered
         */
        data class OnEthSidechainDeposit(
            val hash: String,
            val user: String,
            val amount: BigInteger
        ) : EthereumEvent()

        /**
         * Event which occures when custodian deposits ERC20 token
         * @param hash transaction hash
         * @param user user name in Iroha
         * @param token token name
         * @param amount amount of tokens
         */
        data class OnEthSidechainDepositToken(
            val hash: String,
            val user: String,
            val token: String,
            val amount: BigInteger
        ) : EthereumEvent()
    }
}
