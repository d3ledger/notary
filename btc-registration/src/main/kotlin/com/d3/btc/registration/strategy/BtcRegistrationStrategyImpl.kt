package com.d3.btc.registration.strategy

import com.d3.btc.provider.BtcFreeAddressesProvider
import com.d3.btc.provider.account.IrohaBtcAccountRegistrator
import com.d3.commons.registration.RegistrationStrategy
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

//Strategy for registering BTC addresses
@Component
class BtcRegistrationStrategyImpl(
    @Autowired private val btcFreeAddressesProvider: BtcFreeAddressesProvider,
    @Autowired private val irohaBtcAccountCreator: IrohaBtcAccountRegistrator
) : RegistrationStrategy {

    /**
     * Registers new Iroha client and associates BTC address to it
     * @param accountName - client name
     * @param domainId - client domain
     * @param whitelist - list of bitcoin addresses
     * @param publicKey - client public key
     * @return associated BTC address
     */
    @Synchronized
    override fun register(
        accountName: String,
        domainId: String,
        whitelist: List<String>,
        publicKey: String
    ): Result<String, Exception> {
        return btcFreeAddressesProvider.getFreeAddresses().flatMap { freeAddresses ->
            if (freeAddresses.isEmpty()) {
                throw IllegalStateException("no free btc address to register")
            }
            val freeAddress = freeAddresses.first()
            irohaBtcAccountCreator.create(
                freeAddress.address,
                whitelist,
                accountName,
                domainId,
                publicKey,
                freeAddress.info.notaryKeys,
                btcFreeAddressesProvider.nodeId
            )
        }
    }

    /**
     * Get number of free addresses.
     */
    override fun getFreeAddressNumber(): Result<Int, Exception> {
        return btcFreeAddressesProvider.getFreeAddresses().map { freeAddresses ->
            freeAddresses.size
        }
    }

}
