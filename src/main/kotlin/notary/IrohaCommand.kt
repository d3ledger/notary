package notary

/**
 * Class represents transactions [Notary] can send to [sideChain.iroha.IrohaConsumer]
 */
sealed class IrohaCommand {

    /**
     * Class represents addAssetQuantity Iroha command
     * @param accountId is account id
     * @param assetId is asset id
     * @param amount is a string representation of amount
     */
    class commandAddAssetQuantity(
        val accountId: String,
        val assetId: String,
        val amount: String
    ) : IrohaCommand()

    /**
     * Class represents setAccountDetail Iroha command
     * @param accountId is an account id
     * @param key is a key
     * @param value is a value
     */
    class commandSetAccountDetail(
        val accountId: String,
        val key: String,
        val value: String
    ) : IrohaCommand()

    /**
     * Class represents createAsset Iroha command
     * @param assetName is an asset name
     * @param domainId is a domain id
     * @param precision is a precision
     */
    class commandCreateAsset(
        val assetName: String,
        val domainId: String,
        val precision: Short
    ) : IrohaCommand()

    /**
     * Class represents transferAsset Iroha command
     * @param srcAccountId is a source account
     * @param destAccountId is a destination account
     * @param assetId is an asset id
     * @param description is a desciption
     * @param amount is a string representation of amount
     */
    class commandTransferAsset(
        val srcAccountId: String,
        val destAccountId: String,
        val assetId: String,
        val description: String,
        val amount: String
    ) : IrohaCommand()

    /**
     * Class represents addSignatory Iroha command
     * @param accountId is an account id
     * @param publicKey is a public key associated to the account
     */
    class commandAddSignatory(
        val accountId: String,
        val publicKey: String
    ) : IrohaCommand()
}