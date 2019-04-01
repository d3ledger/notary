package com.d3.btc.fee

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.squareup.moshi.Moshi
import jp.co.soramitsu.iroha.java.QueryAPI
import mu.KLogging
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.sidechain.iroha.util.getAccountDetails
import com.d3.commons.util.irohaEscape
import com.d3.commons.util.irohaUnEscape

const val BTC_FEE_RATE_KEY = "btcFeeRate"
private val feeRateJsonAdapter = Moshi.Builder().build().adapter(FeeRate::class.java)

/**
 * Service that is used to update Bitcoin fee rate in Iroha
 */
class BtcFeeRateService(
    private val btcFeeRateConsumer: IrohaConsumer,
    private val btcFeeRateAccountId: String,
    private val queryAPI: QueryAPI
) {

    /**
     * Sets fee rate if it's possible
     * @param feeRate - new fee rate
     * @return result of operation
     */
    fun setFeeRate(feeRate: FeeRate): Result<Unit, Exception> {
        //Get previous fee rate
        return getAccountDetails(
            queryAPI,
            btcFeeRateAccountId,
            btcFeeRateAccountId
        ).map { details ->
            // If fee rate was set
            details[BTC_FEE_RATE_KEY]?.let { feeRateJson ->
                val prevFeeRate = FeeRate.fromJson(feeRateJson)!!
                // We can update fee rate if it's not older that previous
                return@map prevFeeRate.blockTime < feeRate.blockTime
            }
            // We are able to update, if there is no fee rate in Iroha
            true
        }.map { ableToUpdateFeeRate ->
            if (!ableToUpdateFeeRate) {
                logger.warn { "Not able to update fee rate" }
                return@map
            }
            // Set new fee rate
            ModelUtil.setAccountDetail(
                btcFeeRateConsumer,
                btcFeeRateConsumer.creator,
                BTC_FEE_RATE_KEY,
                feeRate.toJson()
            )
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}

/**
 * Fee rate data class. Holds average fee rate and block time(time it was taken from blockchain)
 */
data class FeeRate(val avgFeeRate: Int = -1, val blockTime: Long = -1) {
    fun isSet() = this.avgFeeRate >= 0

    fun toJson() = feeRateJsonAdapter.toJson(this).irohaEscape()

    companion object {
        fun fromJson(json: String) = feeRateJsonAdapter.fromJson(json.irohaUnEscape())
    }
}
