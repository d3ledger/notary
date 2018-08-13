@file:JvmName("NotaryMain")

package notary

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.EthereumPasswords
import config.loadConfigs
import mu.KLogging
import provider.EthRelayProviderIrohaImpl
import provider.EthTokensProviderImpl
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil

/**
 * Application entry point
 */
fun main(args: Array<String>) {
    val logger = KLogging()

    val notaryConfig = loadConfigs("notary", NotaryConfig::class.java)
    val passwordConfig =
        loadConfigs("notary", EthereumPasswords::class.java, "/ethereum_password.properties")

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
            NotaryInitialization(
                keypair,
                notaryConfig,
                passwordConfig,
                ethRelayProvider,
                ethTokensProvider
            ).init()
        }
        .failure {
            logger.logger.error { it }
            System.exit(1)
        }
}
