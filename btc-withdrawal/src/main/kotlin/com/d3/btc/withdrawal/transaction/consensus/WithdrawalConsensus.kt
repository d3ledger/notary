package com.d3.btc.withdrawal.transaction.consensus

import com.squareup.moshi.Moshi

private val withdrawalConsensusJsonAdapter = Moshi.Builder().build().adapter(WithdrawalConsensus::class.java)

/**
 * Data class that holds all the information to decide how withdrawal transaction must be created
 */
data class WithdrawalConsensus(val availableHeight: Int, val peers: Int) {

    fun toJson() = withdrawalConsensusJsonAdapter.toJson(this)

    companion object {

        /**
         * Turns JSON string into withdrawal
         * @param json - withdrawal data in JSON format
         * @return withdrawal data
         */
        fun fromJson(json: String) =
            withdrawalConsensusJsonAdapter.fromJson(json)!!

        /**
         * Takes consensus data from all the notaries and creates common consensus solution
         * @param consensusData - consensus data from all the notaries
         * @return common consensus
         */
        fun createCommonConsensus(consensusData: List<WithdrawalConsensus>): WithdrawalConsensus {
            if (consensusData.isEmpty()) {
                throw IllegalStateException("Cannot create withdrawal consensus")
            }
            val commonAvailableHeight =
                consensusData.minBy { withdrawalConsensus -> withdrawalConsensus.availableHeight }!!.availableHeight
            val commonPeers = consensusData.minBy { withdrawalConsensus -> withdrawalConsensus.peers }!!.peers
            return WithdrawalConsensus(commonAvailableHeight, commonPeers)
        }
    }
}
