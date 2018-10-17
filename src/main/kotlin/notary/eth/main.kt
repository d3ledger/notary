@file:JvmName("EthNotaryMain")

package notary.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.EthereumPasswords
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
    val notaryConfig = loadConfigs("eth-notary", EthNotaryConfig::class.java, "/eth/notary.properties").get()
    val passwordConfig = loadEthPasswords("eth-notary", "/eth/ethereum_password.properties", args).get()

    executeNotary(passwordConfig, notaryConfig)
}

fun executeNotary(
    ethereumPasswords: EthereumPasswords,
    notaryConfig: EthNotaryConfig
) {
    IrohaInitialization.loadIrohaLibrary()
        .flatMap {
            ModelUtil.loadKeypair(
                notaryConfig.notaryCredential.pubkeyPath,
                notaryConfig.notaryCredential.privkeyPath
            )
        }
        .map { keypair -> IrohaCredential(notaryConfig.notaryCredential.accountId, keypair) }
        .flatMap { irohaCredential ->
            executeNotary(irohaCredential, ethereumPasswords, notaryConfig)
        }
        .failure { ex ->
            logger.error("Cannot run eth notary", ex)
            System.exit(1)
        }
}

/** Run notary instance with particular [irohaCredential] */
fun executeNotary(
    irohaCredential: IrohaCredential,
    ethereumPasswords: EthereumPasswords,
    notaryConfig: EthNotaryConfig
): Result<Unit, Exception> {
    logger.info { "Run ETH notary" }

    val irohaNetwork = IrohaNetworkImpl(
        notaryConfig.iroha.hostname,
        notaryConfig.iroha.port
    )
    val ethRelayProvider = EthRelayProviderIrohaImpl(
        irohaNetwork,
        irohaCredential,
        irohaCredential.accountId,
        notaryConfig.registrationServiceIrohaAccount
    )
    val ethTokensProvider = EthTokensProviderImpl(
        irohaCredential,
        irohaNetwork,
        notaryConfig.tokenStorageAccount,
        notaryConfig.tokenSetterAccount
    )
    return EthNotaryInitialization(
        irohaCredential,
        irohaNetwork,
        notaryConfig,
        ethereumPasswords,
        ethRelayProvider,
        ethTokensProvider
    ).init()
}
