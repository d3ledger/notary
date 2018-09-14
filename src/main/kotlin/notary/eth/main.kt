@file:JvmName("EthNotaryMain")

package notary.eth

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.loadConfigs
import config.loadEthPasswords
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
            logger.error("Cannot run eth notary", ex)
            System.exit(1)
        }
}
