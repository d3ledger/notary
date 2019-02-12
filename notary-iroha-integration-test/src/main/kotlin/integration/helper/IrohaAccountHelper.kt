package integration.helper

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.IrohaCredentialConfig
import config.loadConfigs
import integration.TestConfig
import iroha.protocol.Primitive
import jp.co.soramitsu.iroha.java.IrohaAPI
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import java.security.KeyPair

/**
 * Class that handles all the accounts in running configuration.
 */
class IrohaAccountHelper(private val irohaAPI: IrohaAPI, private val peers: Int = 1) {

    private val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties").get()

    /** A tester Iroha account with permissions to do everything */
    private val testCredential = IrohaCredential(
        testConfig.testCredentialConfig.accountId,
        ModelUtil.loadKeypair(
            testConfig.testCredentialConfig.pubkeyPath,
            testConfig.testCredentialConfig.privkeyPath
        ).get()
    )

    private val irohaConsumer by lazy { IrohaConsumerImpl(testCredential, irohaAPI) }

    /** Notary account */
    val notaryAccount by lazy { createNotaryAccount() }

    /** Notary accounts. Can be used to test multisig */
    val notaryAccounts by lazy {
        val accounts = ArrayList<IrohaCredential>(peers)
        accounts.add(notaryAccount)
        for (peer in 2..peers) {
            val keyPair = ModelUtil.generateKeypair()
            val irohaCredential = IrohaCredential(notaryAccount.accountId, keyPair)
            ModelUtil.addSignatory(irohaConsumer, notaryAccount.accountId, keyPair.public)
                .failure { ex -> throw ex }
            accounts.add(irohaCredential)
        }
        if (peers > 1) {
            ModelUtil.setAccountQuorum(irohaConsumer, notaryAccount.accountId, peers)
                .failure { ex -> throw ex }
        }
        accounts
    }

    /** Notary keys */
    val notaryKeys = mutableListOf(testCredential.keyPair)

    /** Account that used to store registered clients.*/
    val registrationAccount by lazy {
        createTesterAccount("registration", "registration_service", "client")
    }

    /** Account that used to store registered clients in mst fashion.*/
    val mstRegistrationAccount by lazy {
        createTesterAccount("mst_registration", "registration_service", "client")
    }

    /** Account that used to execute transfer commands */
    val btcWithdrawalAccount by lazy {
        createTesterAccount("btc_withdrawal", "withdrawal", "rollback")
    }

    /** Account that collects withdrawal transaction signatures */
    val btcWithdrawalSignatureCollectorAccount by lazy {
        createTesterAccount("signature_collector", "signature_collector")
    }

    /** Account that stores current fee rate */
    val btcFeeRateAccount by lazy {
        createTesterAccount("fee_rate", "btc_fee_rate_setter")
    }

    /** Account that used to store tokens */
    val tokenStorageAccount by lazy { notaryAccount }

    /** Account that sets tokens */
    val tokenSetterAccount by lazy { createTesterAccount("eth_tokens", "eth_token_list_storage") }

    /** Account that used to store peers*/
    val notaryListSetterAccount by lazy { createTesterAccount("notary_setter", "eth_token_list_storage") }

    val notaryListStorageAccount by lazy { createTesterAccount("notary_storage", "notary_list_holder") }

    val changeAddressesStorageAccount by lazy { createTesterAccount("change_addresses") }

    fun createCredentialConfig(credential: IrohaCredential): IrohaCredentialConfig {
        return object : IrohaCredentialConfig {
            override val pubkeyPath: String
                get() = testConfig.testCredentialConfig.pubkeyPath
            override val privkeyPath: String
                get() = testConfig.testCredentialConfig.privkeyPath
            override val accountId: String
                get() = credential.accountId
        }
    }

    /**
     * Creates randomly named tester account in Iroha
     */
    private fun createTesterAccount(prefix: String, vararg roleName: String): IrohaCredential {
        val name = prefix + "_${String.getRandomString(9)}"
        val domain = "notary"
        // TODO - Bulat - generate new keys for account?

        ModelUtil.createAccount(
            irohaConsumer,
            name,
            domain,
            testCredential.keyPair.public,
            *roleName
        ).fold({
            logger.info("account $name@$domain was created")
            return IrohaCredential("$name@$domain", testCredential.keyPair)
        }, { ex ->
            throw ex
        })
    }

    /**
     * Create notary account and grant set_my_quorum, transfer_my_assets and add_my_signatory permissions to test account
     */
    private fun createNotaryAccount(): IrohaCredential {
        val credential = createTesterAccount("notary_${String.getRandomString(9)}", "notary")

        ModelUtil.grantPermissions(
            IrohaConsumerImpl(credential, irohaAPI),
            testCredential.accountId,
            listOf(
                Primitive.GrantablePermission.can_set_my_quorum,
                Primitive.GrantablePermission.can_add_my_signatory,
                Primitive.GrantablePermission.can_transfer_my_assets
            )
        ).failure { throw it }

        return credential
    }

    /**
     * Add signatory with [keypair] to notary
     */
    fun addNotarySignatory(keypair: KeyPair) {
        ModelUtil.addSignatory(irohaConsumer, notaryAccount.accountId, keypair.public)
            .flatMap {
                ModelUtil.setAccountQuorum(irohaConsumer, notaryAccount.accountId, notaryKeys.size + 1)
            }
            .fold({
                notaryKeys.add(keypair)
                logger.info("added signatory to account $notaryAccount")
            }, { ex ->
                throw ex
            })
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
