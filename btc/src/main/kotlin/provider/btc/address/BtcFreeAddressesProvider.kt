package provider.btc.address

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map

// Class that used to fetch free addresses(addresses that might be taken by clients)
class BtcFreeAddressesProvider(
    private val btcAddressesProvider: BtcAddressesProvider,
    private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider
) {

    /**
     * Returns list of free(not taken by clients) addresses
     */
    fun getFreeAddresses(): Result<List<BtcAddress>, Exception> {
        return btcAddressesProvider.getAddresses()
            .fanout { btcRegisteredAddressesProvider.getRegisteredAddresses() }
            .map { (allAddresses, registeredAddresses) ->
                val freeAddresses =
                    allAddresses.filter { address ->
                        !registeredAddresses.any { registeredAddress -> registeredAddress.address == address.address }
                    }
                freeAddresses
            }
    }
}
