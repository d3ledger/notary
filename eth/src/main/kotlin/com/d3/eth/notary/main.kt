@file:JvmName("EthNotaryMain")

package com.d3.eth.notary

import com.github.kittinunf.result.*
import com.d3.commons.config.EthereumPasswords
import com.d3.commons.config.loadConfigs
import com.d3.commons.config.loadEthPasswords
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import com.d3.commons.model.IrohaCredential
import mu.KLogging
import com.d3.eth.provider.EthRelayProviderIrohaImpl
import com.d3.eth.provider.EthTokensProviderImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil

private val logger = KLogging().logger

/**
 * Application entry point
 */
fun main(args: Array<String>) {
    loadConfigs("eth-notary", EthNotaryConfig::class.java, "/eth/notary.properties")
        .fanout { loadEthPasswords("eth-notary", "/eth/ethereum_password.properties", args) }
        .map { (notaryConfig, ethereumPasswords) ->
            executeNotary(ethereumPasswords, notaryConfig)
        }
        .failure { ex ->
            logger.error("Cannot run eth notary", ex)
            System.exit(1)
        }
}

fun executeNotary(
    ethereumPasswords: EthereumPasswords,
    notaryConfig: EthNotaryConfig
) {
    ModelUtil.loadKeypair(
        notaryConfig.notaryCredential.pubkeyPath,
        notaryConfig.notaryCredential.privkeyPath
    )
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

    val irohaAPI = IrohaAPI(
        notaryConfig.iroha.hostname,
        notaryConfig.iroha.port
    )

    val queryAPI = QueryAPI(
        irohaAPI,
        irohaCredential.accountId,
        irohaCredential.keyPair
    )

    val ethRelayProvider = EthRelayProviderIrohaImpl(
        queryAPI,
        irohaCredential.accountId,
        notaryConfig.registrationServiceIrohaAccount
    )
    val ethTokensProvider = EthTokensProviderImpl(
        queryAPI,
        notaryConfig.tokenStorageAccount,
        notaryConfig.tokenSetterAccount
    )
    return EthNotaryInitialization(
        irohaCredential,
        irohaAPI,
        notaryConfig,
        ethereumPasswords,
        ethRelayProvider,
        ethTokensProvider
    ).init()
}
