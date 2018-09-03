@file:JvmName("EthNotaryMain")

package notary.eth

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.EthereumPasswords
import config.loadConfigs
import mu.KLogging
import provider.eth.EthRelayProviderIrohaImpl
import provider.eth.EthTokensProviderImpl
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

/**
 * Application entry point
 */
fun main(args: Array<String>) {
    val notaryConfig = loadConfigs("eth-notary", EthNotaryConfig::class.java, "/eth/notary.properties")
    executeNotary(notaryConfig)
}

fun executeNotary(notaryConfig: EthNotaryConfig) {
    val logger = KLogging()
    val passwordConfig =
        loadConfigs("eth-notary", EthereumPasswords::class.java, "/eth/ethereum_password.properties")

    IrohaInitialization.loadIrohaLibrary()
        .flatMap { ModelUtil.loadKeypair(notaryConfig.iroha.pubkeyPath, notaryConfig.iroha.privkeyPath) }
        .flatMap { keypair ->
            val ethRelayProvider = EthRelayProviderIrohaImpl(
                notaryConfig.iroha,
                keypair,
                notaryConfig.iroha.creator,
                notaryConfig.registrationServiceIrohaAccount
            )
            val ethTokensProvider = EthTokensProviderImpl(
                notaryConfig.iroha,
                keypair,
                notaryConfig.iroha.creator,
                notaryConfig.tokenStorageAccount
            )
            EthNotaryInitialization(
                keypair,
                notaryConfig,
                passwordConfig,
                ethRelayProvider,
                ethTokensProvider
            ).init()
        }
        .failure { ex ->
            logger.logger.error("cannot run eth notary", ex)
            System.exit(1)
        }
}
