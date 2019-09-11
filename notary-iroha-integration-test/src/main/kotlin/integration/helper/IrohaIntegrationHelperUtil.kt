/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.helper

import com.d3.chainadapter.client.RMQConfig
import com.d3.chainadapter.client.ReliableIrohaChainListener
import com.d3.commons.config.loadConfigs
import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.commons.util.getRandomString
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import integration.TestConfig
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Transaction
import kotlinx.coroutines.runBlocking
import mu.KLogging
import java.io.Closeable
import java.math.BigDecimal
import java.security.KeyPair

const val D3_DOMAIN = "d3"

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
    val rmqConfig =
        loadRawLocalConfigs("rmq", RMQConfig::class.java, "rmq.properties")
    val testQueue = String.getRandomString(20)

    val testCredential = IrohaCredential(testConfig.testCredentialConfig)

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
    val queryHelper by lazy {
        IrohaQueryHelperImpl(
            irohaAPI,
            testCredential.accountId,
            testCredential.keyPair
        )
    }

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

    fun getAccountDetails(
        accountDetailHolder: String,
        accountDetailSetter: String
    ): Map<String, String> {
        return queryHelper.getAccountDetails(
            accountDetailHolder,
            accountDetailSetter
        ).get()
    }

    /**
     * Return [account] data.
     */
    fun getAccount(account: String): String {
        return queryHelper.getAccount(account).get().toString()
    }

    /**
     * Return signatories of an account.
     */
    fun getSignatories(accountId: String): List<String> {
        return queryHelper.getSignatories(accountId).get()
    }

    /**
     * Names current thread
     * @param name - name of thread
     */
    fun nameCurrentThread(name: String) {
        Thread.currentThread().name = name
    }

    /**
     * Add asset to Iroha account
     * Add asset to creator and then transfer to destination account.
     * @param accountId - destination account
     * @param assetId - asset to add
     * @param amount - amount to add
     */
    fun addIrohaAssetTo(accountId: String, assetId: String, amount: String) {
        ModelUtil.addAssetIroha(irohaConsumer, assetId, amount).failure { ex -> throw ex }
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
        return queryHelper.getAccountAsset(
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
        createdTime: Long = System.currentTimeMillis(),
        // first is for user, second is for brvs instance
        quorum: Int = 2
    ): String {
        logger.info { "Iroha transfer of $amount $assetId from $srcAccountId to $destAccountId" }
        val tx = Transaction.builder(creator)
            .transferAsset(srcAccountId, destAccountId, assetId, description, amount)
            .setCreatedTime(createdTime)
            .setQuorum(quorum)
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
     * Send transfer asset command in Iroha with fee
     * @param creator - iroha transaction creator
     * @param kp - keypair
     * @param srcAccountId - source account id
     * @param destAccountId - destination account id
     * @param assetId - asset id
     * @param description - transaction description
     * @param amount - amount
     * @param feeAssetId - fee asset id
     * @param feeAmount - amount of fee
     * @param createdTime - time tx creation. Current by default.
     * @return hex representation of transaction hash
     */
    fun transferAssetIrohaFromClientWithFee(
        creator: String,
        kp: KeyPair,
        srcAccountId: String,
        destAccountId: String,
        assetId: String,
        description: String,
        amount: String,
        feeAssetId: String,
        feeAmount: String,
        feeDescription: String = "transfer fee",
        createdTime: Long = System.currentTimeMillis(),
        // first is for user, second is for brvs instance
        quorum: Int = 2
    ): String {
        logger.info { "Iroha transfer of $amount $assetId from $srcAccountId to $destAccountId. Fee $feeAmount" }
        val tx = Transaction.builder(creator)
            .transferAsset(srcAccountId, destAccountId, assetId, description, amount)
            .transferAsset(srcAccountId, destAccountId, feeAssetId, feeDescription, feeAmount)
            .setCreatedTime(createdTime)
            .setQuorum(quorum)
            .sign(kp)
            .build()
        return irohaConsumer.send(tx).get()
    }

    /**
     * Query Iroha account balance from [accountId].
     * @return Map(assetId to balance)
     */
    fun getAccountAssets(
        accountId: String
    ): Map<String, String> {
        return queryHelper.getAccountAssets(accountId).get()
    }

    /**
     * Send SetAccountDetail to Iroha
     * A one should use this method if the creator of tx is client account
     * @param irohaConsumer - iroha network layer
     * @param accountId - account to set details
     * @param key - key of detail
     * @param value - value of detail
     * @return hex representation of transaction hash
     */
    fun setAccountDetailWithRespectToBrvs(
        irohaConsumer: IrohaConsumer,
        accountId: String,
        key: String,
        value: String,
        createdTime: Long = System.currentTimeMillis()
    ): Result<String, Exception> {
        return setAccountDetail(
            irohaConsumer,
            accountId,
            key,
            value,
            createdTime,
            2
        )
    }

    /**
     * Send SetAccountDetail to Iroha
     * @param irohaConsumer - iroha network layer
     * @param accountId - account to set details
     * @param key - key of detail
     * @param value - value of detail
     * @return hex representation of transaction hash
     */
    fun setAccountDetail(
        irohaConsumer: IrohaConsumer,
        accountId: String,
        key: String,
        value: String,
        createdTime: Long = System.currentTimeMillis(),
        quorum: Int = 1
    ): Result<String, Exception> {
        return ModelUtil.setAccountDetail(
            irohaConsumer,
            accountId,
            key,
            value,
            createdTime,
            // first is for user, second is for brvs instance
            quorum
        )
    }

    /**
     * Create Iroha asset
     */
    fun createAsset(
        assetName: String = String.getRandomString(7),
        assetDomain: String = "test",
        precision: Int = 18
    ): Result<String, Exception> {
        return ModelUtil.createAsset(
            accountHelper.irohaConsumer,
            assetName,
            assetDomain,
            precision
        ).map {
            logger.info { "Token $assetName#$assetDomain was created" }
            "$assetName#$assetDomain"
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
