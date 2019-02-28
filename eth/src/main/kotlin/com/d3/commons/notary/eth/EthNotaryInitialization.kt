package com.d3.commons.notary.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import com.d3.commons.config.EthereumPasswords
import io.reactivex.Observable
import iroha.protocol.Primitive
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import jp.co.soramitsu.iroha.java.Transaction
import com.d3.commons.model.IrohaCredential
import mu.KLogging
import com.d3.commons.notary.Notary
import com.d3.commons.notary.NotaryImpl
import com.d3.commons.notary.endpoint.RefundServerEndpoint
import com.d3.commons.notary.endpoint.ServerInitializationBundle
import com.d3.commons.notary.endpoint.eth.EthRefundStrategyImpl
import okhttp3.OkHttpClient
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import com.d3.commons.provider.NotaryPeerListProviderImpl
import com.d3.commons.provider.eth.EthRelayProvider
import com.d3.commons.provider.eth.EthTokensProvider
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.sidechain.eth.EthChainHandler
import com.d3.commons.sidechain.eth.EthChainListener
import com.d3.commons.sidechain.eth.util.BasicAuthenticator
import java.math.BigInteger

const val ENDPOINT_ETHEREUM = "eth"

/**
 * Class for notary instantiation
 * @param ethRelayProvider - provides with white list of ethereum wallets
 * @param ethTokensProvider - provides with white list of ethereum ERC20 tokens
 */
class EthNotaryInitialization(
    private val notaryCredential: IrohaCredential,
    private val irohaAPI: IrohaAPI,
    private val ethNotaryConfig: EthNotaryConfig,
    private val passwordsConfig: EthereumPasswords,
    private val ethRelayProvider: EthRelayProvider,
    private val ethTokensProvider: EthTokensProvider
) {
    /**
     * Init notary
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Eth notary initialization" }
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
        val web3 = Web3j.build(HttpService(ethNotaryConfig.ethereum.url, builder.build(), false))

        /** List of all observable wallets */
        val ethHandler = EthChainHandler(web3, ethRelayProvider, ethTokensProvider)
        return EthChainListener(
            web3,
            BigInteger.valueOf(ethNotaryConfig.ethereum.confirmationPeriod)
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
                    ethNotaryConfig.withdrawalAccountId,
                    Primitive.GrantablePermission.can_transfer_my_assets
                )
                .sign(notaryCredential.keyPair)
                .build()
        )

        val queryAPI = QueryAPI(irohaAPI, notaryCredential.accountId, notaryCredential.keyPair)

        val peerListProvider = NotaryPeerListProviderImpl(
            queryAPI,
            ethNotaryConfig.notaryListStorageAccount,
            ethNotaryConfig.notaryListSetterAccount
        )

        return NotaryImpl(notaryCredential, irohaAPI, ethEvents, peerListProvider)
    }

    /**
     * Init refund notary.endpoint
     */
    private fun initRefund() {
        logger.info { "Init Refund endpoint" }
        RefundServerEndpoint(
            ServerInitializationBundle(ethNotaryConfig.refund.port, ENDPOINT_ETHEREUM),
            EthRefundStrategyImpl(
                ethNotaryConfig,
                irohaAPI,
                notaryCredential,
                ethNotaryConfig.ethereum,
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
