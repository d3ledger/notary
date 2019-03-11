package integration.helper

import com.d3.commons.config.RMQConfig
import com.d3.commons.config.getConfigFolder
import com.d3.commons.config.loadConfigs
import com.d3.commons.config.loadRawConfigs
import integration.TestConfig
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import jp.co.soramitsu.iroha.java.Transaction
import kotlinx.coroutines.runBlocking
import com.d3.commons.model.IrohaCredential
import mu.KLogging
import com.d3.commons.sidechain.iroha.ReliableIrohaChainListener
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.sidechain.iroha.util.getAccountAsset
import com.d3.commons.util.getRandomString
import java.io.Closeable
import java.math.BigDecimal
import java.security.KeyPair
import java.security.PublicKey

/**
 * Utility class that makes testing more comfortable
 */
open class IrohaIntegrationHelperUtil(private val peers: Int = 1) : Closeable {

    override fun close() {
        irohaAPI.close()
        if (irohaChainListenerDelegate.isInitialized()) {
            irohaListener.close()
        }
    }

    val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties").get()
    val rmqConfig = loadRawConfigs("rmq", RMQConfig::class.java, "${getConfigFolder()}/rmq.properties")
    val testQueue = String.getRandomString(20)

    val testCredential = IrohaCredential(
        testConfig.testCredentialConfig.accountId,
        ModelUtil.loadKeypair(
            testConfig.testCredentialConfig.pubkeyPath,
            testConfig.testCredentialConfig.privkeyPath
        ).get()
    )

    open val accountHelper by lazy { IrohaAccountHelper(irohaAPI, peers) }

    open val configHelper by lazy {
        IrohaConfigHelper()
    }

    val irohaAPI by lazy {
        IrohaAPI(testConfig.iroha.hostname, testConfig.iroha.port)
    }

    val irohaConsumer by lazy {
        IrohaConsumerImpl(testCredential, irohaAPI)
    }

    /**
     * TODO this is not very safe to use this thing
     * Tester account has all the permissions, while accounts that will be used
     * in production may be out of some crucial permissions by mistake.
     */
    val queryAPI by lazy { QueryAPI(irohaAPI, testCredential.accountId, testCredential.keyPair) }

    private val irohaChainListenerDelegate = lazy {
        ReliableIrohaChainListener(
            rmqConfig,
            testQueue
        )
    }

    private val irohaListener by irohaChainListenerDelegate

    protected val registrationConsumer by lazy {
        IrohaConsumerImpl(accountHelper.registrationAccount, irohaAPI)
    }

    protected val notaryListIrohaConsumer by lazy {
        IrohaConsumerImpl(accountHelper.notaryListSetterAccount, irohaAPI)
    }

    /**
     * Purge all iroha blocks, call and wait for exactly one iroha block
     */
    fun purgeAndwaitOneIrohaBlock(func: () -> Unit) {
        runBlocking {
            irohaListener.purge()
            func()
            val (block, _) = irohaListener.getBlock()
            logger.info { "Wait for one block ${block.blockV1.payload.height}" }
        }
    }

    fun getAccountDetails(accountDetailHolder: String, accountDetailSetter: String): Map<String, String> {
        return com.d3.commons.sidechain.iroha.util.getAccountDetails(
            queryAPI,
            accountDetailHolder,
            accountDetailSetter
        ).get()
    }

    /**
     * Return [account] data.
     */
    fun getAccount(account: String): String {
        return com.d3.commons.sidechain.iroha.util.getAccountData(
            queryAPI,
            account
        ).get().toString()
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
     * Creates dummy setAccountDetail transaction
     */
    fun createDummyTransaction(key: String, value: String) {
        ModelUtil.setAccountDetail(
            accountHelper.irohaConsumer,
            accountHelper.testCredential.accountId,
            key,
            value
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
     * @param createdTime - time tx creation. Current by default.
     * @return hex representation of transaction hash
     */
    fun transferAssetIrohaFromClient(
        creator: String,
        kp: KeyPair,
        srcAccountId: String,
        destAccountId: String,
        assetId: String,
        description: String,
        amount: String,
        createdTime: Long = System.currentTimeMillis()
    ): String {
        val tx = Transaction.builder(creator)
            .transferAsset(srcAccountId, destAccountId, assetId, description, amount)
            .setCreatedTime(createdTime).sign(kp)
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
     * Query Iroha account balance from [accountId].
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
