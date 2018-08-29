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
import sidechain.eth.util.hashToWithdraw
import sidechain.eth.util.signUserData
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.getAccountDetails

class NotaryException(reason: String) : Exception(reason)

/**
 * Class performs effective implementation of refund strategy for Ethereum
 */
class EthRefundStrategyImpl(
    val irohaConfig: IrohaConfig,
    val irohaNetwork: IrohaNetwork,
    val ethereumConfig: EthereumConfig,
    val ethereumPasswords: EthereumPasswords,
    private val keypair: Keypair,
    private val whitelistSetter: String
) : EthRefundStrategy {

    override fun performRefund(request: EthRefundRequest): EthNotaryResponse {
        logger.info("check tx ${request.irohaTx} for refund")

        return ModelUtil.getTransaction(irohaNetwork, irohaConfig.creator, keypair, request.irohaTx)
            .flatMap { checkTransaction(it, request) }
            .flatMap { makeRefund(it) }
            .fold({ it },
                { EthNotaryResponse.Error(it.toString()) })
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

                    EthRefund(destEthAddress, "mockCoinType", "10", request.irohaTx)
                }
            // withdrawal case
                (appearedTx.payload.reducedPayload.commandsCount == 1) &&
                        commands.hasTransferAsset() -> {
                    val destAccount = commands.transferAsset.destAccountId
                    if (destAccount != irohaConfig.creator)
                        throw NotaryException("Refund - check transaction. Destination account is wrong '$destAccount'")

                    val amount = commands.transferAsset.amount
                    val token = commands.transferAsset.assetId.dropLastWhile { it != '#' }.dropLast(1)
                    val destEthAddress = commands.transferAsset.description

                    val srcAccountId = commands.transferAsset.srcAccountId
                    checkWithdrawalAddress(srcAccountId, destEthAddress)
                        .fold(
                            {
                                if (!it) {
                                    logger.warn { "$destEthAddress not in whitelist" }
                                    throw NotaryException("$destEthAddress not in whitelist")
                                }
                            },
                            { throw it }
                        )

                    EthRefund(destEthAddress, token, amount, request.irohaTx)
                }
                else -> {
                    logger.error { "Transaction doesn't contain expected commands." }
                    throw NotaryException("Transaction doesn't contain expected commands.")
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
        logger.info { "Make refund. Address: ${ethRefund.address}, amount: ${ethRefund.amount} ${ethRefund.assetId}, hash: ${ethRefund.irohaTxHash}" }
        return Result.of {
            val finalHash =
                hashToWithdraw(
                    ethRefund.assetId,
                    ethRefund.amount,
                    ethRefund.address,
                    ethRefund.irohaTxHash
                )
            val signature = signUserData(ethereumConfig, ethereumPasswords, finalHash)
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
            irohaConfig,
            keypair,
            irohaNetwork,
            srcAccountId,
            whitelistSetter
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
