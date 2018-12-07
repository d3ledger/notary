@file:JvmName("BtcSendBtc")

package withdrawal.btc.send

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.IrohaCredentialConfig
import config.loadConfigs
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import withdrawal.btc.config.BtcWithdrawalConfig

/*
    This is an utility file that may be used to send some money.
    Mostly for testing purposes.
 */

private const val BTC_ASSET_ID = "btc#bitcoin"
private val logger = KLogging().logger

/*
    Sends args[0] amount of SAT to args[0] address
 */
fun main(args: Array<String>) {
    val destAddress = args[0]
    val satAmount = args[1].toInt()
    IrohaInitialization.loadIrohaLibrary()
        .flatMap {
            loadConfigs("btc-withdrawal", BtcWithdrawalConfig::class.java, "/btc/withdrawal.properties")
        }
        .map { withdrawalConfig ->
            val irohaNetwork = IrohaNetworkImpl(withdrawalConfig.iroha.hostname, withdrawalConfig.iroha.port)
            sendBtc(
                destAddress, satAmount,
                withdrawalConfig.withdrawalCredential.accountId,
                IrohaConsumerImpl(createNotaryCredential(withdrawalConfig.notaryCredential), irohaNetwork)
            ).failure { ex ->
                logger.error("Cannot send BTC", ex)
                System.exit(1)
            }
        }
}

// Creates notary credential
private fun createNotaryCredential(
    notaryCredential: IrohaCredentialConfig
): IrohaCredential {
    val notaryKeypair = ModelUtil.loadKeypair(
        notaryCredential.pubkeyPath,
        notaryCredential.privkeyPath
    ).fold(
        { keypair -> keypair },
        { ex -> throw ex })
    return IrohaCredential(notaryCredential.accountId, notaryKeypair)
}

/**
 * Sends BTC
 * @param destinationAddress - base58 address to send money
 * @param satAmount - amount of SAT to send
 * @param withdrawalAccountId - withdrawal account id
 * @param notaryConsumer - notary consumer object
 */
private fun sendBtc(
    destinationAddress: String,
    satAmount: Int,
    withdrawalAccountId: String,
    notaryConsumer: IrohaConsumer
): Result<Unit, Exception> {
    return ModelUtil.addAssetIroha(notaryConsumer, BTC_ASSET_ID, satAmount.toString()).flatMap {
        ModelUtil.transferAssetIroha(
            notaryConsumer,
            notaryConsumer.creator,
            withdrawalAccountId,
            BTC_ASSET_ID,
            destinationAddress,
            satAmount.toString()
        )
    }.map { Unit }
}
