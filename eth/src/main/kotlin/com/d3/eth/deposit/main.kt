@file:JvmName("EthDepositMain")

package com.d3.eth.deposit

import com.d3.commons.config.EthereumPasswords
import com.d3.commons.config.loadConfigs
import com.d3.commons.config.loadEthPasswords
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.eth.provider.EthRelayProviderIrohaImpl
import com.d3.eth.provider.EthTokensProviderImpl
import com.github.kittinunf.result.*
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import mu.KLogging

private val logger = KLogging().logger

const val ETH_DEPOSIT_SERVICE_NAME = "eth-deposit"

/**
 * Application entry point
 */
fun main(args: Array<String>) {
    loadConfigs("eth-deposit", EthDepositConfig::class.java, "/eth/deposit.properties")
        .fanout { loadEthPasswords("eth-deposit", "/eth/ethereum_password.properties", args) }
        .map { (depositConfig, ethereumPasswords) ->
            executeDeposit(ethereumPasswords, depositConfig)
        }
        .failure { ex ->
            logger.error("Cannot run eth deposit", ex)
            System.exit(1)
        }
}

fun executeDeposit(
    ethereumPasswords: EthereumPasswords,
    depositConfig: EthDepositConfig
) {
    ModelUtil.loadKeypair(
        depositConfig.notaryCredential.pubkeyPath,
        depositConfig.notaryCredential.privkeyPath
    )
        .map { keypair -> IrohaCredential(depositConfig.notaryCredential.accountId, keypair) }
        .flatMap { irohaCredential ->
            executeDeposit(irohaCredential, ethereumPasswords, depositConfig)
        }
        .failure { ex ->
            logger.error("Cannot run eth deposit", ex)
            System.exit(1)
        }
}

/** Run deposit instance with particular [irohaCredential] */
fun executeDeposit(
    irohaCredential: IrohaCredential,
    ethereumPasswords: EthereumPasswords,
    depositConfig: EthDepositConfig
): Result<Unit, Exception> {
    logger.info { "Run ETH deposit" }

    val irohaAPI = IrohaAPI(
        depositConfig.iroha.hostname,
        depositConfig.iroha.port
    )

    val queryAPI = QueryAPI(
        irohaAPI,
        irohaCredential.accountId,
        irohaCredential.keyPair
    )

    val ethRelayProvider = EthRelayProviderIrohaImpl(
        queryAPI,
        irohaCredential.accountId,
        depositConfig.registrationServiceIrohaAccount
    )
    val ethTokensProvider = EthTokensProviderImpl(
        queryAPI,
        depositConfig.tokenStorageAccount,
        depositConfig.tokenSetterAccount
    )
    return EthDepositInitialization(
        irohaCredential,
        irohaAPI,
        depositConfig,
        ethereumPasswords,
        ethRelayProvider,
        ethTokensProvider
    ).init()
}
