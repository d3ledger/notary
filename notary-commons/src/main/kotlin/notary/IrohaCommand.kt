package notary

import java.security.PublicKey

/**
 * Class represents commands that [Notary] can send to [sidechain.iroha.consumer.IrohaConsumer]
 */
sealed class IrohaCommand {

    /**
     * Class represents addAssetQuantity Iroha command
     * @param assetId asset id to add value to
     * @param amount is a string representation of amount to add
     */
    data class CommandAddAssetQuantity(
        val assetId: String,
        val amount: String
    ) : IrohaCommand()

    /**
     * Class represents createAccount Iroha command
     * @param accountName - name for account
     * @param domainId - target domain id
     * @param mainPubkey - ed25519 public key to add to the account
     */
    data class CommandCreateAccount(
        val accountName: String,
        val domainId: String,
        val mainPubkey: PublicKey
    ) : IrohaCommand()

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
        val precision: Int
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
        val publicKey: PublicKey
    ) : IrohaCommand()

    /**
     * Class represents addPeer Iroha command
     * @param address peer's address, ip and port
     * @param peerKey peer's key
     */
    data class CommandAddPeer(
        val address: String,
        val peerKey: PublicKey
    ) : IrohaCommand()
}
