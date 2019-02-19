package com.d3.btc.registration.strategy

import com.d3.btc.provider.BtcFreeAddressesProvider
import com.d3.btc.provider.account.IrohaBtcAccountCreator
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import registration.RegistrationStrategy

//Strategy for registering BTC addresses
@Component
class BtcRegistrationStrategyImpl(
    @Autowired private val btcFreeAddressesProvider: BtcFreeAddressesProvider,
    @Autowired private val irohaBtcAccountCreator: IrohaBtcAccountCreator
) : RegistrationStrategy {

    /**
     * Registers new Iroha client and associates BTC address to it
     * @param name - client name
     * @param pubkey - client public key
     * @param whitelist - list of bitcoin addresses
     * @return associated BTC address
     */
    @Synchronized
    override fun register(
        name: String,
        domain: String,
        whitelist: List<String>,
        pubkey: String
    ): Result<String, Exception> {
        return btcFreeAddressesProvider.getFreeAddresses().flatMap { freeAddresses ->
            if (freeAddresses.isEmpty()) {
                throw IllegalStateException("no free btc address to register")
            }
            val freeAddress = freeAddresses.first()
            irohaBtcAccountCreator.create(
                freeAddress.address,
                whitelist,
                name,
                domain,
                pubkey,
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
