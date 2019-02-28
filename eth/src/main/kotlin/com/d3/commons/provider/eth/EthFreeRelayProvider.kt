package com.d3.commons.provider.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.QueryAPI
import mu.KLogging
import com.d3.commons.sidechain.iroha.util.getAccountDetails

/**
 * Provides with free ethereum relay wallet
 * @param queryAPI - iroha queries network layer
 * @param notaryIrohaAccount - Master notary account in Iroha to write down the information about free relay wallets has been added
 */
// TODO Prevent double relay accounts usage (in perfect world it is on Iroha side with custom code). In real world
// on provider side with some synchronization.
class EthFreeRelayProvider(
    private val queryAPI: QueryAPI,
    private val notaryIrohaAccount: String,
    private val registrationIrohaAccount: String
) {

    init {
        logger.info {
            "Init free relay provider with holder account '$notaryIrohaAccount' and setter account '$registrationIrohaAccount'"
        }
    }

    /**
     * Get first free ethereum relay wallet.
     * @return free ethereum relay wallet
     */
    fun getRelay(): Result<String, Exception> {
        return getRelays().map { freeWallets ->
            if (freeWallets.isEmpty())
                throw IllegalStateException("EthFreeRelayProvider - no free relay wallets created by $registrationIrohaAccount")
            freeWallets.first()
        }
    }

    /**
     * Get all free Ethereum relay wallets
     * @return free Ethereum relay wallets
     */
    fun getRelays(): Result<Set<String>, Exception> {
        return getAccountDetails(
            queryAPI,
            notaryIrohaAccount,
            registrationIrohaAccount
        ).map { relays ->
            relays.filterValues { irohaAccount -> irohaAccount == "free" }.keys
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
