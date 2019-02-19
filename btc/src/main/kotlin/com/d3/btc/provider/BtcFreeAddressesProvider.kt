package com.d3.btc.provider

import com.d3.btc.model.BtcAddress
import com.d3.btc.provider.address.BtcAddressesProvider
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map

// Class that used to fetch free addresses(addresses that might be taken by clients)
class BtcFreeAddressesProvider(
    val nodeId: String,
    private val btcAddressesProvider: BtcAddressesProvider,
    private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider
) {

    /**
     * Returns list of free(not taken by clients) addresses
     */
    fun getFreeAddresses(): Result<List<BtcAddress>, Exception> {
        return btcAddressesProvider.getAddresses().fanout { btcRegisteredAddressesProvider.getRegisteredAddresses() }
            .map { (addresses, takenAddresses) ->
                addresses
                    .filter { btcAddress ->
                        btcAddress.info.nodeId == nodeId &&
                                !takenAddresses.any { takenAddress -> takenAddress.address == btcAddress.address }
                    }
            }
    }
}
