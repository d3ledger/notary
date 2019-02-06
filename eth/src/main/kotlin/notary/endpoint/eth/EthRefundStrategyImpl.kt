package notary.endpoint.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.flatMap
import config.EthereumConfig
import config.EthereumPasswords
import iroha.protocol.TransactionOuterClass.Transaction
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import model.IrohaCredential
import mu.KLogging
import notary.eth.EthNotaryConfig
import org.web3j.crypto.ECKeyPair
import provider.eth.EthRelayProviderIrohaImpl
import provider.eth.EthTokensProvider
import sidechain.eth.util.DeployHelper
import sidechain.eth.util.hashToWithdraw
import sidechain.eth.util.signUserData
import sidechain.iroha.util.getSingleTransaction
import java.math.BigDecimal

class NotaryException(reason: String) : Exception(reason)

/**
 * Class performs effective implementation of refund strategy for Ethereum
 */
class EthRefundStrategyImpl(
    notaryConfig: EthNotaryConfig,
    irohaAPI: IrohaAPI,
    private val credential: IrohaCredential,
    ethereumConfig: EthereumConfig,
    ethereumPasswords: EthereumPasswords,
    private val tokensProvider: EthTokensProvider
) : EthRefundStrategy {
    private val queryAPI = QueryAPI(irohaAPI, credential.accountId, credential.keyPair)
    private val relayProvider = EthRelayProviderIrohaImpl(
        queryAPI,
        credential.accountId,
        notaryConfig.registrationServiceIrohaAccount
    )

    private val whiteListProvider = EthWhiteListProvider(
        notaryConfig.whitelistSetter, queryAPI
    )

    private var ecKeyPair: ECKeyPair = DeployHelper(ethereumConfig, ethereumPasswords).credentials.ecKeyPair

    override fun performRefund(request: EthRefundRequest): EthNotaryResponse {
        logger.info("Check tx ${request.irohaTx} for refund")

        return getSingleTransaction(queryAPI, request.irohaTx)
            .flatMap { checkTransaction(it, request) }
            .flatMap { makeRefund(it) }
            .fold({ it },
                { ex ->
                    logger.error("Cannot perform refund", ex)
                    EthNotaryResponse.Error(ex.toString())
                })
    }

    /**
     * The method checks transaction and create refund if it is correct
     * @param appearedTx - target transaction from Iroha
     * @param request - user's request with transaction hash
     * @return Refund or error
     */
    private fun checkTransaction(
        appearedTx: Transaction,
        request: EthRefundRequest
    ): Result<EthRefund, Exception> {
        return Result.of {
            val commands = appearedTx.payload.reducedPayload.getCommands(0)

            when {
                // rollback case
                appearedTx.payload.reducedPayload.commandsCount == 1 &&
                        commands.hasSetAccountDetail() -> {

                    // TODO a.chernyshov replace with effective implementation
                    // 1. Get eth transaction hash from setAccountDetail
                    // 2. Check eth transaction and get info from it
                    //    There should be a batch with 3 txs
                    //      if 3 tx in the batch - reject rollback
                    //      if 2 tx (transfer asset is absent) - approve rollback
                    // 3. build EthRefund

                    val key = commands.setAccountDetail.key
                    val value = commands.setAccountDetail.value
                    val destEthAddress = ""
                    logger.info { "Rollback case ($key, $value)" }

                    val relayAddress = relayProvider.getRelays().get().filter {
                        it.value == commands.transferAsset.srcAccountId
                    }.keys.first()

                    EthRefund(destEthAddress, "mockCoinType", "10", request.irohaTx, relayAddress)
                }
                // withdrawal case
                (appearedTx.payload.reducedPayload.commandsCount == 1) &&
                        commands.hasTransferAsset() -> {
                    val destAccount = commands.transferAsset.destAccountId
                    // TODO: Bulat change destAccount to withdrawalTrigger account
                    if (destAccount != credential.accountId)
                        throw NotaryException("Refund - check transaction. Destination account is wrong '$destAccount'")

                    val amount = commands.transferAsset.amount
                    val token = commands.transferAsset.assetId.dropLastWhile { it != '#' }.dropLast(1)
                    val destEthAddress = commands.transferAsset.description
                    val srcAccountId = commands.transferAsset.srcAccountId

                    val tokenInfo = tokensProvider.getTokenAddress(token)
                        .fanout { tokensProvider.getTokenPrecision(token) }

                    whiteListProvider.checkWithdrawalAddress(srcAccountId, destEthAddress)
                        .flatMap { isWhitelisted ->
                            if (!isWhitelisted) {
                                val errorMsg = "$destEthAddress not in whitelist"
                                logger.error { errorMsg }
                                throw NotaryException(errorMsg)
                            }
                            relayProvider.getRelay(commands.transferAsset.srcAccountId)
                        }.fanout {
                            tokenInfo
                        }.fold(
                            { (relayAddress, tokenInfo) ->
                                val decimalAmount =
                                    BigDecimal(amount).scaleByPowerOfTen(tokenInfo.second).toPlainString()
                                EthRefund(destEthAddress, tokenInfo.first, decimalAmount, request.irohaTx, relayAddress)
                            },
                            { throw it }
                        )
                }
                else -> {
                    val errorMsg = "Transaction doesn't contain expected commands."
                    logger.error { errorMsg }
                    throw NotaryException(errorMsg)
                }
            }
        }
    }

    /**
     * The method signs refund and return valid parameters for Ethereum smart contract call
     * @param ethRefund - refund for signing
     * @return signed refund or error
     */
    private fun makeRefund(ethRefund: EthRefund): Result<EthNotaryResponse, Exception> {
        logger.info { "Make refund. Asset address: ${ethRefund.assetId}, amount: ${ethRefund.amount}, to address: ${ethRefund.address}, hash: ${ethRefund.irohaTxHash}, relay: ${ethRefund.relayAddress}" }
        return Result.of {
            val finalHash =
                hashToWithdraw(
                    ethRefund.assetId,
                    ethRefund.amount,
                    ethRefund.address,
                    ethRefund.irohaTxHash,
                    ethRefund.relayAddress
                )

            val signature = signUserData(ecKeyPair, finalHash)
            EthNotaryResponse.Successful(signature, ethRefund)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
