@file:JvmName("LongevityMain")

import config.IrohaCredentialConfig
import integration.helper.IntegrationHelperUtil
import jp.co.soramitsu.iroha.ModelCrypto
import model.IrohaCredential
import javax.jws.WebParam

/** Path to public key of 2nd instance of notary */
private val pubkeyPath2 = "deploy/iroha/keys/notary2@notary.pub"

/** Path to private key of 2nd instance of notary */
private val privkeyPath2 = "deploy/iroha/keys/notary2@notary.priv"

/**
 * Entry point for Registration Service
 */
fun main(args: Array<String>) {
    val integrationHelper = IntegrationHelperUtil()


    ///////

    val notaryConfig = integrationHelper.configHelper.createEthNotaryConfig()

    val keypair = ModelCrypto().generateKeypair()
    val credential = IrohaCredential(integrationHelper.accountHelper.notaryAccount.accountId, keypair)

    ///////


    // create 2nd notray config
    val irohaConfig =
        integrationHelper.configHelper.createIrohaConfig()

    val notaryCredential2 = object : IrohaCredentialConfig {
        override val pubkeyPath: String
            get() = pubkeyPath2
        override val privkeyPath: String
            get() = privkeyPath2
        override val accountId: String
            get() = integrationHelper.accountHelper.notaryAccount.accountId
    }

//    val notaryConfig = integrationHelper.configHelper.createEthNotaryConfig(
//        irohaConfig,
//        integrationHelper.configHelper.ethNotaryConfig.ethereum,
//        notaryCredential2
//    )


    // run 2nd instance of notary
    integrationHelper.runEthNotary(notaryConfig)
}
