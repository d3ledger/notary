@file:JvmName("EthNotaryMain")

package notary.eth

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.loadConfigs
import config.loadEthPasswords
import model.IrohaCredential
import mu.KLogging
import provider.eth.EthRelayProviderIrohaImpl
import provider.eth.EthTokensProviderImpl
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaNetworkImpl
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
        .flatMap {
            ModelUtil.loadKeypair(
                notaryConfig.notaryCredential.pubkeyPath,
                notaryConfig.notaryCredential.privkeyPath
            )
        }
        .map { keypair -> IrohaCredential(notaryConfig.notaryCredential.accountId, keypair) }
        .flatMap { credential ->
            val irohaNetwork = IrohaNetworkImpl(
                notaryConfig.iroha.hostname,
                notaryConfig.iroha.port
            )
            val ethRelayProvider = EthRelayProviderIrohaImpl(
                irohaNetwork,
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
