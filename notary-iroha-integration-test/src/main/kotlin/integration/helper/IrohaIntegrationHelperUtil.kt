package integration.helper

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.loadConfigs
import integration.TestConfig
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelTransactionBuilder
import kotlinx.coroutines.runBlocking
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.getAccountAsset
import java.io.Closeable

/**
 * Utility class that makes testing more comfortable
 */
open class IrohaIntegrationHelperUtil : Closeable {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure { ex ->
                logger.error("Cannot load Iroha library", ex)
                System.exit(1)
            }
    }

    override fun close() {
        irohaNetwork.close()
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

    open val accountHelper by lazy { IrohaAccountHelper(irohaNetwork) }

    open val configHelper by lazy {
        IrohaConfigHelper()
    }

    val irohaNetwork by lazy {
        IrohaNetworkImpl(testConfig.iroha.hostname, testConfig.iroha.port)
    }

    protected val irohaConsumer by lazy {
        IrohaConsumerImpl(testCredential, irohaNetwork)
    }

    protected val irohaListener = IrohaChainListener(
        testConfig.iroha.hostname,
        testConfig.iroha.port,
        testCredential
    )

    protected val registrationConsumer by lazy {
        IrohaConsumerImpl(accountHelper.registrationAccount, irohaNetwork)
    }

    protected val notaryListIrohaConsumer by lazy {
        IrohaConsumerImpl(accountHelper.notaryListSetterAccount, irohaNetwork)
    }

    /**
     * Waits for exactly one iroha block
     */
    fun waitOneIrohaBlock() {
        runBlocking {
            val block = irohaListener.getBlock()
            logger.info { "Wait for one block ${block.payload.height}" }
        }
    }

    fun getAccountDetails(accountDetailHolder: String, accountDetailSetter: String): Map<String, String> {
        return sidechain.iroha.util.getAccountDetails(
            testCredential,
            irohaNetwork,
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
            ModelUtil.transferAssetIroha(irohaConsumer, irohaConsumer.creator, accountId, assetId, "", amount)
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
            testCredential,
            irohaNetwork,
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
        kp: Keypair,
        srcAccountId: String,
        destAccountId: String,
        assetId: String,
        description: String,
        amount: String
    ): String {
        val utx = ModelTransactionBuilder()
            .creatorAccountId(creator)
            .createdTime(ModelUtil.getCurrentTime())
            .transferAsset(srcAccountId, destAccountId, assetId, description, amount)
            .build()
        val hash = utx.hash()
        return ModelUtil.prepareTransaction(utx, kp)
            .flatMap { tx -> irohaNetwork.sendAndCheck(tx, hash) }
            .get()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
