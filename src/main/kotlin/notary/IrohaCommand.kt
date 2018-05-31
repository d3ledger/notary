package notary

import java.util.*

/**
 * Class represents commands that [Notary] can send to [sideChain.iroha.consumer.IrohaConsumer]
 */
sealed class IrohaCommand {

    /**
     * Class represents addAssetQuantity Iroha command
     * @param accountId account id to add asset quantity to
     * @param assetId asset id to add value to
     * @param amount is a string representation of amount to add
     */
    data class CommandAddAssetQuantity(
            val accountId: String,
            val assetId: String,
            val amount: String
    ) : IrohaCommand() {
    }

    /**
     * Class represents setAccountDetail Iroha command
     * @param accountId account id to add detail to
     * @param key detail key
     * @param value detail value
     */
    data class CommandSetAccountDetail(
            val accountId: String,
            val key: String,
            val value: String
    ) : IrohaCommand()

    /**
     * Class represents createAsset Iroha command
     * @param assetName - asset name to create
     * @param domainId - domain id to create asset in
     * @param precision - asset precision
     */
    data class CommandCreateAsset(
            val assetName: String,
            val domainId: String,
            val precision: Short
    ) : IrohaCommand()

    /**
     * Class represents transferAsset Iroha command
     * @param srcAccountId - source account id
     * @param destAccountId - destination account id
     * @param assetId - asset id
     * @param description - description message which user can set
     * @param amount - amount of asset to transfer
     */
    data class CommandTransferAsset(
            val srcAccountId: String,
            val destAccountId: String,
            val assetId: String,
            val description: String,
            val amount: String
    ) : IrohaCommand()

    /**
     * Class represents addSignatory Iroha command
     * @param accountId id of signatory's account
     * @param publicKey public key of signatory
     */
    data class CommandAddSignatory(
            val accountId: String,
            val publicKey: String
    ) : IrohaCommand()

    /**
     * Class represents addPeer Iroha command
     * @param address peer's address, ip and port
     * @param peerKey peer's key
     */
    data class CommandAddPeer(
            val address: String,
            val peerKey: ByteArray
    ) : IrohaCommand() {

        override fun equals(other: Any?): Boolean {
            other as CommandAddPeer
            return address == other.address && Arrays.equals(peerKey, other.peerKey)
        }

        override fun hashCode(): Int =
                Arrays.hashCode(address.toByteArray() + peerKey)


        companion object {
            /**
             * Takes a binary proto command and creates a model command AddPeer
             * @param bytes the command represented as byte array
             */
            fun fromProto(bytes: ByteArray): CommandAddPeer {
                val cmd = iroha.protocol.Commands.Command.parseFrom(bytes).addPeer
                return CommandAddPeer(cmd.peer.address, cmd.peer.peerKey.toByteArray())
            }
        }
    }
}
