package sidechain

import com.google.protobuf.ByteString
import iroha.protocol.Commands
import notary.endpoint.eth.AmountType
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
        data class AddPeer(
            val address: String,
            val key: ByteString
        ) : IrohaEvent() {

            companion object {

                /**
                 * Generate [AddPeer] from proto
                 */
                fun fromProto(cmd: Commands.AddPeer): AddPeer {
                    return AddPeer(cmd.peer.address, cmd.peer.peerKey)
                }
            }

        }

        /**
         * Event which is raised when custodian transfer assets to notary account to withdraw asset
         *
         * @param srcAccount source of transfer
         * @param dstAccount destination of transfer
         * @param asset is asset id in Iroha
         * @param amount of ethereum to withdraw
         * @param description description field of transfer
         * @param hash hash of transaction in Iroha
         */
        data class SideChainTransfer(
            val srcAccount: String,
            val dstAccount: String,
            val asset: String,
            val amount: AmountType,
            val description: String,
            val hash: String
        ) : IrohaEvent() {

            companion object {

                /**
                 * Generate [SideChainTransfer] from proto
                 */
                fun fromProto(cmd: Commands.TransferAsset, hash: String): SideChainTransfer {
                    return SideChainTransfer(
                        cmd.srcAccountId, cmd.destAccountId,
                        cmd.assetId, cmd.amount, cmd.description, hash
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
         * @param from - from ethereum address
         */
        data class OnEthSidechainDeposit(
            val hash: String,
            val user: String,
            val amount: BigInteger,
            val from: String
        ) : EthereumEvent()

        /**
         * Event which occures when custodian deposits ERC20 token
         * @param hash transaction hash
         * @param user user name in Iroha
         * @param token token name
         * @param amount amount of tokens
         * @param from - from ethereum address
         */
        data class OnEthSidechainDepositToken(
            val hash: String,
            val user: String,
            val token: String,
            val amount: BigInteger,
            val from: String
        ) : EthereumEvent()
    }
}
