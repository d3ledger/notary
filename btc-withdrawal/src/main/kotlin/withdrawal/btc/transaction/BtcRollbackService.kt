package withdrawal.btc.transaction

import com.d3.btc.helper.currency.satToBtc
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import provider.NotaryPeerListProvider
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.util.ModelUtil

private const val BTC_ASSET_ID = "btc#bitcoin"

/**
 * Bitcoin rollback service
 */
@Component
class BtcRollbackService(
    @Qualifier("withdrawalConsumer")
    @Autowired private val withdrawalConsumer: IrohaConsumer,
    @Autowired private val peerListProvider: NotaryPeerListProvider
) {

    /**
     * Rollbacks given amount of money to a particular Iroha account
     * @param accountId - Iroha account id, which money will be restored
     * @param amountSat - amount of money to rollback in SAT format
     * @param withdrawalTime - time of withdrawal that needs a rollback. Used in multisig.
     */
    fun rollback(accountId: String, amountSat: Long, withdrawalTime: Long) {
        ModelUtil.transferAssetIroha(
            withdrawalConsumer,
            withdrawalConsumer.creator,
            accountId,
            BTC_ASSET_ID,
            "rollback",
            satToBtc(amountSat).toPlainString(),
            withdrawalTime,
            peerListProvider.getPeerList().size
        ).fold(
            { logger.info { "Rollback(accountId:$accountId, amount:${satToBtc(amountSat).toPlainString()}) was committed" } },
            { ex -> logger.error("Cannot perform rollback", ex) })
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
