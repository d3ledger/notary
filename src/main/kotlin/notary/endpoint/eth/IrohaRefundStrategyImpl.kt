package notary.endpoint.eth

import com.github.kittinunf.result.map
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.ModelUtil

class IrohaRefundStrategyImpl(
    private val irohaNetwork: IrohaNetwork,
    private val credential: IrohaCredential,
    private val notaryAccountId: String,
    private val irohaConsumer: IrohaConsumer
) : IrohaRefundStrategy {

    override fun performRefund(request: IrohaRefundRequest): IrohaNotaryResponse {
        return ModelUtil.getTransaction(irohaNetwork, credential, request.irohaTx)
            .map { tx ->
                tx.payload.reducedPayload.commandsList.first { command ->
                    val transferAsset = command.transferAsset
                    transferAsset?.srcAccountId != "" && transferAsset?.destAccountId == notaryAccountId
                }
            }
            .map { transferCommand ->
                val destAccountId = transferCommand?.transferAsset?.srcAccountId
                        ?: throw IllegalStateException("Unable to identify primary Iroha transaction data")

                ModelUtil.transferAssetIroha(
                    irohaConsumer,
                    notaryAccountId,
                    destAccountId,
                    transferCommand.transferAsset.assetId,
                    "Rollback transaction due to failed withdrawal in Ethereum",
                    transferCommand.transferAsset.amount
                )
                    .fold({ txHash ->
                        logger.info("Successfully sent rollback transaction to Iroha, hash: $txHash")
                        IrohaNotaryResponse.Successful(txHash)
                    }, { ex: Exception ->
                        logger.error("Error during rollback transfer transaction", ex)
                        IrohaNotaryResponse.Error(ex.message.toString())
                    })
            }.get()
    }

    companion object : KLogging()
}
