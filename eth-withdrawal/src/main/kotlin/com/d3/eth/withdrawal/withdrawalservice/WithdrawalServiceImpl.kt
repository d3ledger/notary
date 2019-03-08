package com.d3.eth.withdrawal.withdrawalservice

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import com.squareup.moshi.Moshi
import io.reactivex.Observable
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import com.d3.commons.model.IrohaCredential
import mu.KLogging
import com.d3.eth.notary.endpoint.BigIntegerMoshiAdapter
import com.d3.eth.notary.endpoint.EthNotaryResponse
import com.d3.eth.notary.endpoint.EthNotaryResponseMoshiAdapter
import com.d3.commons.provider.NotaryPeerListProviderImpl
import com.d3.eth.provider.EthTokensProvider
import com.d3.eth.provider.EthTokensProviderImpl
import com.d3.commons.sidechain.SideChainEvent
import com.d3.eth.sidechain.util.extractVRS
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.sidechain.iroha.util.getAccountDetails
import com.d3.commons.sidechain.iroha.util.getSingleTransaction
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Approval to be passed to the Ethereum for refund
 * @param tokenContractAddress Ethereum address of ERC-20 token (or 0x0000000000000000000000000000000000000000 for ether)
 * @param amount amount of token/ether to transfer
 * @param account target account
 * @param irohaHash hash of approving TransferAsset transaction in Iroha
 * @param r array of r-components of notary signatures
 * @param s array of s-components of notary signatures
 * @param v array of v-components of notary signatures
 * @param relay Ethereum address of user relay contract
 */
data class RollbackApproval(
    val tokenContractAddress: String,
    val amount: String,
    val account: String,
    val irohaHash: String,
    val r: ArrayList<ByteArray>,
    val s: ArrayList<ByteArray>,
    val v: ArrayList<BigInteger>,
    val relay: String
)


/**
 * Implementation of Withdrawal Service
 */
class WithdrawalServiceImpl(
    private val withdrawalServiceConfig: WithdrawalServiceConfig,
    private val credential: IrohaCredential,
    private val irohaAPI: IrohaAPI,
    private val irohaHandler: Observable<SideChainEvent.IrohaEvent>
) : WithdrawalService {

    init {
        logger.info { "Init withdrawal service, irohaCredentials = ${credential.accountId}, notaryAccount = $masterAccount'" }
    }

    private val queryAPI by lazy { QueryAPI(irohaAPI, credential.accountId, credential.keyPair) }
    private val notaryPeerListProvider = NotaryPeerListProviderImpl(
        queryAPI,
        withdrawalServiceConfig.notaryListStorageAccount,
        withdrawalServiceConfig.notaryListSetterAccount
    )
    private val tokensProvider: EthTokensProvider = EthTokensProviderImpl(
        queryAPI,
        withdrawalServiceConfig.tokenStorageAccount,
        withdrawalServiceConfig.tokenSetterAccount
    )

    private val masterAccount = withdrawalServiceConfig.notaryIrohaAccount

    private val irohaConsumer: IrohaConsumer by lazy { IrohaConsumerImpl(credential, irohaAPI) }

    private fun findInAccDetail(acc: String, name: String): Result<String, Exception> {
        return getAccountDetails(
            queryAPI,
            acc,
            withdrawalServiceConfig.registrationIrohaAccount
        ).map { relays ->
            val keys = relays.filterValues { it == name }.keys
            if (keys.isEmpty())
                throw Exception("No relay address in account details $acc bind to $name")
            else
                keys.first()
        }
    }

    /**
     * Query all notaries for approval of refund
     * @param event - iroha transfer event
     * @return rollback approval or exception
     */
    private fun requestNotary(event: SideChainEvent.IrohaEvent.SideChainTransfer): Result<RollbackApproval, Exception> {
        // description field holds target account address
        return tokensProvider.getTokenAddress(event.asset)
            .fanout { tokensProvider.getTokenPrecision(event.asset) }
            .fanout { findInAccDetail(masterAccount, event.srcAccount) }
            .map { (tokenInfo, relayAddress) ->
                val hash = event.hash
                val amount = event.amount
                if (!event.asset.contains("#ethereum") && !event.asset.contains("#sora")) {
                    throw Exception("Incorrect asset name in Iroha event: " + event.asset)
                }

                val address = event.description
                val vv = ArrayList<BigInteger>()
                val rr = ArrayList<ByteArray>()
                val ss = ArrayList<ByteArray>()

                notaryPeerListProvider.getPeerList().forEach { peer ->
                    logger.info { "Query $peer for proof" }
                    val res: khttp.responses.Response
                    try {
                        res = khttp.get("$peer/eth/$hash")
                    } catch (e: Exception) {
                        logger.warn { "Exception was thrown while refund server request: server $peer" }
                        logger.warn { e.localizedMessage }
                        return@forEach
                    }
                    if (res.statusCode != 200) {
                        logger.warn { "Error happened while refund server request: server $peer, error ${res.statusCode}" }
                        return@forEach
                    }

                    val moshi = Moshi
                        .Builder()
                        .add(EthNotaryResponseMoshiAdapter())
                        .add(BigInteger::class.java, BigIntegerMoshiAdapter())
                        .build()!!
                    val ethNotaryAdapter = moshi.adapter(EthNotaryResponse::class.java)!!
                    val response = ethNotaryAdapter.fromJson(res.jsonObject.toString())

                    when (response) {
                        is EthNotaryResponse.Error -> {
                            logger.warn { "EthNotaryResponse.Error: ${response.reason}" }
                            return@forEach
                        }

                        is EthNotaryResponse.Successful -> {
                            val signature = response.ethSignature
                            val vrs = extractVRS(signature)
                            vv.add(vrs.v)
                            rr.add(vrs.r)
                            ss.add(vrs.s)
                        }
                    }
                }
                if (vv.size == 0) {
                    throw Exception("Not a single valid response was received from any refund server")
                }

                val (coinAddress, precision) = tokenInfo
                val decimalAmount = BigDecimal(amount).scaleByPowerOfTen(precision).toPlainString()
                RollbackApproval(coinAddress, decimalAmount, address, hash, rr, ss, vv, relayAddress)
            }
    }

    /**
     * Handle IrohaEvent
     * @param irohaEvent - iroha event
     * @return withdrawal service output event or exception
     */
    override fun onIrohaEvent(irohaEvent: SideChainEvent.IrohaEvent): Result<List<WithdrawalServiceOutputEvent>, Exception> {
        when (irohaEvent) {
            is SideChainEvent.IrohaEvent.SideChainTransfer -> {
                logger.info { "Iroha transfer event to ${irohaEvent.dstAccount}" }

                if (irohaEvent.dstAccount == masterAccount) {
                    logger.info { "Withdrawal event" }
                    return requestNotary(irohaEvent)
                        .map { listOf(WithdrawalServiceOutputEvent.EthRefund(it)) }
                }

                return Result.of { emptyList<WithdrawalServiceOutputEvent>() }
            }
        }
        return Result.error(Exception("Wrong event type or wrong destination account"))
    }

    /**
     * Relay events to consumer
     */
    override fun output(): Observable<Result<List<WithdrawalServiceOutputEvent>, Exception>> {
        return irohaHandler
            .map {
                onIrohaEvent(it)
            }
    }

    override fun returnIrohaAssets(event: WithdrawalServiceOutputEvent): Result<Unit, Exception> {
        if (event !is WithdrawalServiceOutputEvent.EthRefund) {
            return Result.error(IllegalArgumentException("Unsupported output event type"))
        }

        logger.info("Withdrawal rollback initiated: ${event.proof.irohaHash}")
        return getSingleTransaction(queryAPI, event.proof.irohaHash)
            .map { tx ->
                tx.payload.reducedPayload.commandsList.first { command ->
                    val transferAsset = command.transferAsset
                    transferAsset?.srcAccountId != "" && transferAsset?.destAccountId == masterAccount
                }
            }
            .map { transferCommand ->
                val destAccountId = transferCommand?.transferAsset?.srcAccountId
                    ?: throw IllegalStateException("Unable to identify primary Iroha transaction data")

                ModelUtil.transferAssetIroha(
                    irohaConsumer,
                    masterAccount,
                    destAccountId,
                    transferCommand.transferAsset.assetId,
                    "Rollback transaction due to failed withdrawal in Ethereum",
                    transferCommand.transferAsset.amount
                )
                    .fold({ txHash ->
                        logger.info("Successfully sent rollback transaction to Iroha, hash: $txHash")
                    }, { ex: Exception ->
                        logger.error("Error during rollback transfer transaction", ex)
                        throw ex
                    })
            }

    }

    /**
     * Logger
     */
    companion object : KLogging()
}
