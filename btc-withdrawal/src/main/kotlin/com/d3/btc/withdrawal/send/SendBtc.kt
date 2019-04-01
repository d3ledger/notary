@file:JvmName("BtcSendBtc")

package com.d3.btc.withdrawal.send

import com.d3.btc.helper.currency.satToBtc
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import com.d3.commons.config.IrohaCredentialConfig
import com.d3.commons.config.loadConfigs
import jp.co.soramitsu.iroha.java.IrohaAPI
import com.d3.commons.model.IrohaCredential
import mu.KLogging
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.btc.withdrawal.config.BtcWithdrawalConfig

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
    val btcAmount = satToBtc(args[1].toLong())
    loadConfigs("btc-withdrawal", BtcWithdrawalConfig::class.java, "/btc/withdrawal.properties")
        .map { withdrawalConfig ->
            val irohaNetwork = IrohaAPI(withdrawalConfig.iroha.hostname, withdrawalConfig.iroha.port)
            sendBtc(
                destAddress, btcAmount.toPlainString(),
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
 * @param btcAmount - amount of BTC to send
 * @param withdrawalAccountId - withdrawal account id
 * @param notaryConsumer - notary consumer object
 */
private fun sendBtc(
    destinationAddress: String,
    btcAmount: String,
    withdrawalAccountId: String,
    notaryConsumer: IrohaConsumer
): Result<Unit, Exception> {
    return ModelUtil.addAssetIroha(notaryConsumer, BTC_ASSET_ID, btcAmount).flatMap {
        ModelUtil.transferAssetIroha(
            notaryConsumer,
            notaryConsumer.creator,
            withdrawalAccountId,
            BTC_ASSET_ID,
            destinationAddress,
            btcAmount
        )
    }.map { Unit }
}
