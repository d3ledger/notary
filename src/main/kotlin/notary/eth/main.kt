@file:JvmName("EthNotaryMain")

package notary.eth

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.loadConfigs
import config.loadEthPasswords
import model.IrohaCredential
import mu.KLogging
import provider.eth.EthRelayProviderIrohaImpl
import provider.eth.EthTokensProviderImpl
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

private val logger = KLogging().logger

/**
 * Application entry point
 */
fun main(args: Array<String>) {
    val notaryConfig = loadConfigs("eth-notary", EthNotaryConfig::class.java, "/eth/notary.properties")
    executeNotary(notaryConfig, args)
}

fun executeNotary(notaryConfig: EthNotaryConfig, args: Array<String> = emptyArray()) {
    logger.info { "Run ETH notary" }
    val passwordConfig = loadEthPasswords("eth-notary", "/eth/ethereum_password.properties", args)
    val credential = IrohaCredential(
        notaryConfig.notaryCredential.accountId,
        ModelUtil.loadKeypair(notaryConfig.notaryCredential.pubkeyPath, notaryConfig.notaryCredential.privkeyPath).get()
    )


    IrohaInitialization.loadIrohaLibrary()
        .flatMap {
            val ethRelayProvider = EthRelayProviderIrohaImpl(
                notaryConfig.iroha,
                credential,
                credential.accountId,
                notaryConfig.registrationServiceIrohaAccount
            )
            val ethTokensProvider = EthTokensProviderImpl(
                notaryConfig.iroha,
                credential,
                notaryConfig.tokenStorageAccount,
                notaryConfig.tokenSetterAccount
            )
            EthNotaryInitialization(
                credential,
                notaryConfig,
                passwordConfig,
                ethRelayProvider,
                ethTokensProvider
            ).init()
        }
        .failure { ex ->
            logger.error("Cannot run eth notary", ex)
            System.exit(1)
        }
}
