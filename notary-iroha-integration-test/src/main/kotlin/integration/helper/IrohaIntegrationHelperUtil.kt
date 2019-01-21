package integration.helper

import config.loadConfigs
import integration.TestConfig
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import jp.co.soramitsu.iroha.java.Transaction
import kotlinx.coroutines.runBlocking
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.getAccountAsset
import java.io.Closeable
import java.math.BigDecimal
import java.security.KeyPair
import java.security.PublicKey

/**
 * Utility class that makes testing more comfortable
 */
open class IrohaIntegrationHelperUtil : Closeable {

    override fun close() {
        irohaAPI.close()
        irohaListener.close()
    }

    val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties").get()

    val testCredential = IrohaCredential(
        testConfig.testCredentialConfig.accountId,
        ModelUtil.loadKeypair(
            testConfig.testCredentialConfig.pubkeyPath,
            testConfig.testCredentialConfig.privkeyPath
        ).get()
    )

    open val accountHelper by lazy { IrohaAccountHelper(irohaAPI) }

    open val configHelper by lazy {
        IrohaConfigHelper()
    }

    val irohaAPI by lazy {
        IrohaAPI(testConfig.iroha.hostname, testConfig.iroha.port)
    }

    val irohaConsumer by lazy {
        IrohaConsumerImpl(testCredential, irohaAPI)
    }

    val queryAPI by lazy { QueryAPI(irohaAPI, testCredential.accountId, testCredential.keyPair) }


    protected val irohaListener = IrohaChainListener(
        testConfig.iroha.hostname,
        testConfig.iroha.port,
        testCredential
    )

    protected val registrationConsumer by lazy {
        IrohaConsumerImpl(accountHelper.registrationAccount, irohaAPI)
    }

    protected val notaryListIrohaConsumer by lazy {
        IrohaConsumerImpl(accountHelper.notaryListSetterAccount, irohaAPI)
    }

    /**
     * Waits for exactly one iroha block
     */
    fun waitOneIrohaBlock() {
        runBlocking {
            val block = irohaListener.getBlock()
            logger.info { "Wait for one block ${block.blockV1.payload.height}" }
        }
    }

    fun getAccountDetails(accountDetailHolder: String, accountDetailSetter: String): Map<String, String> {
        return sidechain.iroha.util.getAccountDetails(
            queryAPI,
            accountDetailHolder,
            accountDetailSetter
        ).get()
    }

    /**
     * Add asset to Iroha account
     * Add asset to creator and then transfer to destination account.
     * @param accountId - destination account
     * @param assetId - asset to add
     * @param amount - amount to add
     */
    fun addIrohaAssetTo(accountId: String, assetId: String, amount: String) {
        ModelUtil.addAssetIroha(irohaConsumer, assetId, amount)
        if (irohaConsumer.creator != accountId)
            ModelUtil.transferAssetIroha(
                irohaConsumer,
                irohaConsumer.creator,
                accountId,
                assetId,
                "",
                amount
            )
    }

    /**
     * Add asset to Iroha account
     * Add asset to creator and then transfer to destination account.
     * @param accountId - destination account
     * @param assetId - asset to add
     * @param amount - amount to add
     */
    fun addIrohaAssetTo(accountId: String, assetId: String, amount: BigDecimal) {
        addIrohaAssetTo(accountId, assetId, amount.toPlainString())
    }

    /**
     * Returns balance in Iroha
     * Query Iroha account balance
     * @param accountId - account in Iroha
     * @param assetId - asset in Iroha
     * @param credential - credential of query creator
     * @return balance of account asset
     */
    fun getIrohaAccountBalance(accountId: String, assetId: String): String {
        return getAccountAsset(
            queryAPI,
            accountId,
            assetId
        ).get()
    }

    /**
     * Add notary to notary list provider. [name] is a string to identify a multisig notary account
     */
    fun addNotary(name: String, address: String) {
        ModelUtil.setAccountDetail(
            notaryListIrohaConsumer,
            accountHelper.notaryListStorageAccount.accountId,
            name,
            address
        )
    }

    /**
     * Transfer asset in iroha with custom creator
     * @param creator - iroha transaction creator
     * @param kp - keypair
     * @param srcAccountId - source account id
     * @param destAccountId - destination account id
     * @param assetId - asset id
     * @param description - transaction description
     * @param amount - amount
     * @return hex representation of transaction hash
     */
    fun transferAssetIrohaFromClient(
        creator: String,
        kp: KeyPair,
        srcAccountId: String,
        destAccountId: String,
        assetId: String,
        description: String,
        amount: String
    ): String {
        val tx = Transaction.builder(creator)
            .transferAsset(srcAccountId, destAccountId, assetId, description, amount)
            .sign(kp)
            .build()
        return irohaConsumer.send(tx).get()
    }

    /**
     * Transfer asset in iroha with custom creator
     * @param creator - iroha transaction creator
     * @param kp - keypair
     * @param srcAccountId - source account id
     * @param destAccountId - destination account id
     * @param assetId - asset id
     * @param description - transaction description
     * @param amount - amount
     * @return hex representation of transaction hash
     */
    fun transferAssetIrohaFromClient(
        creator: String,
        kp: KeyPair,
        srcAccountId: String,
        destAccountId: String,
        assetId: String,
        description: String,
        amount: BigDecimal
    ): String {
        return transferAssetIrohaFromClient(
            creator,
            kp,
            srcAccountId,
            destAccountId,
            assetId,
            description,
            amount.toPlainString()
        )
    }

    /**
     * Create iroha account [name]@[domain] with [pubkey]
     */
    fun createAccount(name: String, domain: String, pubkey: PublicKey) {
        ModelUtil.createAccount(irohaConsumer, name, domain, pubkey)
    }

    /**
     * Query Iroha account balance from [accountId]. Creator is [credential].
     * @return Map(assetId to balance)
     */
    fun getAccountAssets(
        accountId: String
    ): Map<String, String> {
        return queryAPI.getAccountAssets(accountId).accountAssetsList.associate { asset ->
            asset.assetId to asset.balance
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
