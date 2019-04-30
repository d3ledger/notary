package com.d3.eth.provider

import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import mu.KLogging

/**
 * Provides with free ethereum relay wallet
 * @param queryHelper - iroha queries network layer
 * @param notaryIrohaAccount - Master notary account in Iroha to write down the information about free relay wallets has been added
 */
// TODO Prevent double relay accounts usage (in perfect world it is on Iroha side with custom code). In real world
// on provider side with some synchronization.
class EthFreeRelayProvider(
    private val queryHelper: IrohaQueryHelper,
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
        return queryHelper.getAccountDetails(
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
