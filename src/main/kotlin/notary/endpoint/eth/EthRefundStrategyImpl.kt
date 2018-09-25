package notary.endpoint.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.EthereumConfig
import config.EthereumPasswords
import config.IrohaConfig
import iroha.protocol.TransactionOuterClass.Transaction
import jp.co.soramitsu.iroha.Keypair
import mu.KLogging
import notary.eth.EthNotaryConfig
import org.web3j.crypto.ECKeyPair
import provider.eth.EthRelayProviderIrohaImpl
import provider.eth.EthTokensProvider
import sidechain.eth.util.*
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.getAccountDetails
import java.math.BigDecimal

class NotaryException(reason: String) : Exception(reason)

/**
 * Class performs effective implementation of refund strategy for Ethereum
 */
class EthRefundStrategyImpl(
    private val notaryConfig: EthNotaryConfig,
    private val irohaNetwork: IrohaNetwork,
    ethereumPasswords: EthereumPasswords,
    private val keypair: Keypair,
    private val tokensProvider: EthTokensProvider
) : EthRefundStrategy {

    val relayProvider = EthRelayProviderIrohaImpl(
        notaryConfig.iroha,
     keypair,
     notaryConfig.iroha.creator,
        notaryConfig.registrationServiceIrohaAccount)

    private var ecKeyPair: ECKeyPair = DeployHelper(notaryConfig.ethereum, ethereumPasswords).credentials.ecKeyPair

    override fun performRefund(request: EthRefundRequest): EthNotaryResponse {
        logger.info("Check tx ${request.irohaTx} for refund")

        return ModelUtil.getTransaction(irohaNetwork, notaryConfig.iroha.creator, keypair, request.irohaTx)
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

                    println(relayAddress)

                    EthRefund(destEthAddress, "mockCoinType", "10", request.irohaTx, relayAddress)
                }
                // withdrawal case
                (appearedTx.payload.reducedPayload.commandsCount == 1) &&
                        commands.hasTransferAsset() -> {
                    val destAccount = commands.transferAsset.destAccountId
                    if (destAccount != notaryConfig.iroha.creator)
                        throw NotaryException("Refund - check transaction. Destination account is wrong '$destAccount'")

                    val amount = commands.transferAsset.amount
                    val token = commands.transferAsset.assetId.dropLastWhile { it != '#' }.dropLast(1)
                    val coins = tokensProvider.getTokens().get().toMutableMap()
                    val coinAddress = findInTokens(token, coins)
                    val precision = getPrecision(token, coins)

                    val destEthAddress = commands.transferAsset.description

                    val srcAccountId = commands.transferAsset.srcAccountId
                    checkWithdrawalAddress(srcAccountId, destEthAddress)
                        .fold(
                            { isWhitelisted ->
                                if (!isWhitelisted) {
                                    val errorMsg = "$destEthAddress not in whitelist"
                                    logger.error { errorMsg }
                                    throw NotaryException(errorMsg)
                                }
                            },
                            { throw it }
                        )

                    val relayAddress = relayProvider.getRelays().get().filter {
                        it.value == commands.transferAsset.srcAccountId
                    }.keys.first()
                    println("From Iroha $relayAddress")
                    println("EthDestAcc $destEthAddress")
                    val decimalAmount = BigDecimal(amount).scaleByPowerOfTen(precision.toInt()).toPlainString()

                    EthRefund(destEthAddress, coinAddress, decimalAmount, request.irohaTx, relayAddress)
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
        logger.info { "Make refund. Address: ${ethRefund.address}, relay: ${ethRefund.relayAddress}, amount: ${ethRefund.amount} ${ethRefund.assetId}, hash: ${ethRefund.irohaTxHash}" }
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
     * Check if [srcAccountId] has Ethereum withdrawal [address] in whitelist
     * @param srcAccountId - Iroha account - holder of whitelist
     * @param address - ethereum address to check
     * @return true if whitelist is not set, otherwise checks if [address] in the whitelist
     */
    private fun checkWithdrawalAddress(srcAccountId: String, address: String): Result<Boolean, Exception> {
        return getAccountDetails(
            notaryConfig.iroha,
            keypair,
            irohaNetwork,
            srcAccountId,
            notaryConfig.whitelistSetter
        ).map { details ->
            val whitelist = details["eth_whitelist"]

            if (whitelist == null || whitelist.isEmpty())
                true
            else
                whitelist.split(", ").contains(address)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
