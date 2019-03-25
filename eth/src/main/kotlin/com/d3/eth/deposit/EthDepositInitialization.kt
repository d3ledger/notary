/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.deposit

import com.d3.commons.config.EthereumPasswords
import com.d3.commons.model.IrohaCredential
import com.d3.commons.notary.Notary
import com.d3.commons.notary.NotaryImpl
import com.d3.commons.notary.endpoint.ServerInitializationBundle
import com.d3.commons.provider.NotaryPeerListProviderImpl
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.util.createPrettyScheduledThreadPool
import com.d3.eth.deposit.endpoint.EthRefundStrategyImpl
import com.d3.eth.deposit.endpoint.RefundServerEndpoint
import com.d3.eth.provider.EthRelayProvider
import com.d3.eth.provider.EthTokensProvider
import com.d3.eth.sidechain.EthChainHandler
import com.d3.eth.sidechain.EthChainListener
import com.d3.eth.sidechain.util.BasicAuthenticator
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import io.reactivex.Observable
import iroha.protocol.Primitive
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import jp.co.soramitsu.iroha.java.Transaction
import mu.KLogging
import okhttp3.OkHttpClient
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.JsonRpc2_0Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigInteger

const val ENDPOINT_ETHEREUM = "eth"

/**
 * Class for deposit instantiation
 * @param ethRelayProvider - provides with white list of ethereum wallets
 * @param ethTokensProvider - provides with white list of ethereum ERC20 tokens
 */
class EthDepositInitialization(
    private val notaryCredential: IrohaCredential,
    private val irohaAPI: IrohaAPI,
    private val ethDepositConfig: EthDepositConfig,
    private val passwordsConfig: EthereumPasswords,
    private val ethRelayProvider: EthRelayProvider,
    private val ethTokensProvider: EthTokensProvider
) {
    /**
     * Init notary
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Eth deposit initialization" }
        return initEthChain()
            .map { ethEvent ->
                initNotary(ethEvent)
            }
            .flatMap { notary -> notary.initIrohaConsumer() }
            .map { initRefund() }
    }

    /**
     * Init Ethereum chain listener
     * @return Observable on Ethereum sidechain events
     */
    private fun initEthChain(): Result<Observable<SideChainEvent.PrimaryBlockChainEvent>, Exception> {
        logger.info { "Init Eth chain" }

        val builder = OkHttpClient().newBuilder()
        builder.authenticator(BasicAuthenticator(passwordsConfig))
        val web3 = Web3j.build(
            HttpService(ethDepositConfig.ethereum.url, builder.build(), false),
            JsonRpc2_0Web3j.DEFAULT_BLOCK_TIME.toLong(),
            createPrettyScheduledThreadPool(ETH_DEPOSIT_SERVICE_NAME, "web3j")
        )

        /** List of all observable wallets */
        val ethHandler = EthChainHandler(web3, ethRelayProvider, ethTokensProvider)
        return EthChainListener(
            web3,
            BigInteger.valueOf(ethDepositConfig.ethereum.confirmationPeriod)
        ).getBlockObservable()
            .map { observable ->
                observable.flatMapIterable { ethHandler.parseBlock(it) }
            }
    }

    /**
     * Init Notary
     */
    private fun initNotary(
        ethEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>
    ): Notary {
        logger.info { "Init ethereum notary" }

        irohaAPI.transactionSync(
            Transaction.builder(notaryCredential.accountId)
                .grantPermission(
                    ethDepositConfig.withdrawalAccountId,
                    Primitive.GrantablePermission.can_transfer_my_assets
                )
                .sign(notaryCredential.keyPair)
                .build()
        )

        val queryAPI = QueryAPI(irohaAPI, notaryCredential.accountId, notaryCredential.keyPair)

        val peerListProvider = NotaryPeerListProviderImpl(
            queryAPI,
            ethDepositConfig.notaryListStorageAccount,
            ethDepositConfig.notaryListSetterAccount
        )

        return NotaryImpl(notaryCredential, irohaAPI, ethEvents, peerListProvider)
    }

    /**
     * Init refund notary.endpoint
     */
    private fun initRefund() {
        logger.info { "Init Refund endpoint" }
        RefundServerEndpoint(
            ServerInitializationBundle(ethDepositConfig.refund.port, ENDPOINT_ETHEREUM),
            EthRefundStrategyImpl(
                ethDepositConfig,
                irohaAPI,
                notaryCredential,
                ethDepositConfig.ethereum,
                passwordsConfig,
                ethTokensProvider
            )
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
