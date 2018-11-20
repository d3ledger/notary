package withdrawal.btc.provider

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import org.bitcoinj.core.Address
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import provider.btc.address.BtcAddressesProvider
import provider.btc.network.BtcNetworkConfigProvider

/*
    Class that is used to get change address
 */
@Component
class BtcChangeAddressProvider(
    @Autowired private val btcAddressesProvider: BtcAddressesProvider,
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider
) {
    /**
     * Returns change address
     * @return - result with change address object
     */
    fun getChangeAddress(): Result<Address, Exception> {
        return btcAddressesProvider.getAddresses()
            .map { addresses ->
                Address.fromBase58(
                    btcNetworkConfigProvider.getConfig(),
                    addresses.find { address -> address.isChange() }!!.address
                )
            }
    }

    /**
     * Checks if change address was set
     * @return result with 'true' value, if change address was set
     */
    fun isAddressPresent(): Result<Boolean, Exception> {
        return btcAddressesProvider.getAddresses()
            .map { addresses ->
                addresses.find { address -> address.isChange() } != null
            }
    }
}
