package com.d3.btc.withdrawal.transaction.consensus

import com.d3.btc.withdrawal.transaction.WithdrawalDetails

/**
 * Storage of withdrawal consensus data
 */
object ConsensusDataStorage {

    // Collection that stores consensus data
    private val consensusData = HashMap<String, WithdrawalDetailsConsensus>()

    /**
     * Adds new withdrawal consensus data if possible
     * @param withdrawalHash - hash of withdrawal that needs consensus data
     * @param withdrawalConsensus - withdrawal consensus data
     * @return true if data was added successfully
     */
    @Synchronized
    fun add(withdrawalHash: String, withdrawalConsensus: WithdrawalConsensus): Boolean {
        if (!consensusData.containsKey(withdrawalHash)) {
            return false
        }
        consensusData[withdrawalHash]!!.consensus.add(withdrawalConsensus)
        return true
    }

    /**
     * Clears consensus data by withdrawal hash
     * @param withdrawalHash - hash of withdrawal which consensus will be removed
     */
    @Synchronized
    fun clear(withdrawalHash: String) {
        consensusData.remove(withdrawalHash)
    }

    /**
     * Returns consensus data by withdrawal hash
     * @param withdrawalHash - hash of withdrawal
     * @return consensus data
     */
    @Synchronized
    fun get(withdrawalHash: String): WithdrawalDetailsConsensus? {
        consensusData[withdrawalHash]?.let { withdrawalDetailsConsensus ->
            // Return new ArrayList to avoid ConcurrentModificationException in tests
            return WithdrawalDetailsConsensus(
                withdrawalDetailsConsensus.withdrawalDetails,
                ArrayList(withdrawalDetailsConsensus.consensus)
            )
        }
        return null
    }

    /**
     * Creates consensus data storage for withdrawal
     * @param withdrawalHash - hash of withdrawal that needs consensus data
     */
    @Synchronized
    fun create(withdrawalDetails: WithdrawalDetails) {
        consensusData[withdrawalDetails.irohaFriendlyHashCode()] =
                WithdrawalDetailsConsensus(withdrawalDetails, ArrayList())
    }
}

/**
 * Data class that holds information about withdrawal details and consensus
 */
data class WithdrawalDetailsConsensus(
    val withdrawalDetails: WithdrawalDetails,
    val consensus: MutableList<WithdrawalConsensus>
)

