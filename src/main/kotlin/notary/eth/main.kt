@file:JvmName("EthNotaryMain")

package notary.eth

import com.github.kittinunf.result.Result
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
    IrohaInitialization.loadIrohaLibrary()
        .flatMap {
            ModelUtil.loadKeypair(
                notaryConfig.notaryCredential.pubkeyPath,
                notaryConfig.notaryCredential.privkeyPath
            )
        }
        .map { keypair -> IrohaCredential(notaryConfig.notaryCredential.accountId, keypair) }
        .flatMap { credential ->
            executeNotary(credential, notaryConfig, args)
        }
        .failure { ex ->
            logger.error("Cannot run eth notary", ex)
            System.exit(1)
        }
}

/** Run notary instance with particular [irohaCredential] */
fun executeNotary(
    irohaCredential: IrohaCredential,
    notaryConfig: EthNotaryConfig,
    args: Array<String> = emptyArray()
): Result<Unit, Exception> {
    logger.info { "Run ETH notary" }
    val passwordConfig = loadEthPasswords("eth-notary", "/eth/ethereum_password.properties", args)

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
        notaryConfig.iroha,
        irohaCredential,
        notaryConfig.tokenStorageAccount,
        notaryConfig.tokenSetterAccount
    )
    return EthNotaryInitialization(
        irohaCredential,
        notaryConfig,
        passwordConfig,
        ethRelayProvider,
        ethTokensProvider
    ).init()
}
