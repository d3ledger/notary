package integration.helper

import config.TestConfig
import config.loadConfigs
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelTransactionBuilder
import mu.KLogging
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import util.getRandomString

//Class that handles all the accounts in integration tests.
class AccountHelper(private val keyPair: Keypair) {

    private val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties")

    private val irohaConsumer by lazy { IrohaConsumerImpl(testConfig.iroha) }

    /** Notary account*/
    val notaryAccount by lazy { createTesterAccount("eth_notary", "notary") }

    /** Notary keys */
    val notaryKeys = mutableListOf(keyPair)

    /** Account that used to store registered clients.*/
    val registrationAccount by lazy {
        createTesterAccount("registration", "registration_service")
    }
    /** Account that used to store registered clients in mst fashion.*/
    val mstRegistrationAccount by lazy {
        createTesterAccount("mst_registration", "registration_service")
    }

    /** Account that used to store tokens */
    val tokenStorageAccount = notaryAccount

    /** Account that sets tokens */
    val tokenSetterAccount by lazy { createTesterAccount("eth_tokens", "token_service") }

    // TODO - D3-348 - dolgopolov.work change to suitable role name
    /** Account that used to store peers*/
    val notaryListSetterAccount by lazy { createTesterAccount("notary_setter", "token_service") }

    val notaryListStorageAccount by lazy { createTesterAccount("notary_storage", "notary_holder") }
    /**
     * Creates randomly named tester account in Iroha
     */
    private fun createTesterAccount(prefix: String, roleName: String = "tester"): String {
        val name = prefix + "_${String.getRandomString(9)}"
        val domain = "notary"
        val creator = testConfig.iroha.creator
        irohaConsumer.sendAndCheck(
            ModelTransactionBuilder()
                .creatorAccountId(creator)
                .createdTime(ModelUtil.getCurrentTime())
                .createAccount(name, domain, keyPair.publicKey())
                .appendRole("$name@$domain", roleName)
                .build()
        ).fold({
            logger.info("account $name@$domain was created")
            return "$name@$domain"
        }, { ex ->
            throw ex
        })
    }

    /**
     * Add signatory with [keypair] to notary
     */
    fun addNotarySignatory(keypair: Keypair) {
        irohaConsumer.sendAndCheck(
            ModelTransactionBuilder()
                .creatorAccountId(notaryAccount)
                .createdTime(ModelUtil.getCurrentTime())
                .addSignatory(notaryAccount, keypair.publicKey())
                .setAccountQuorum(notaryAccount, notaryKeys.size + 1)
                .build()
        ).fold({
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
