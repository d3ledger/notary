package registration.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.flatMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import provider.btc.address.BtcAddressesProvider
import provider.btc.address.BtcRegisteredAddressesProvider
import registration.IrohaAccountCreator
import registration.RegistrationStrategy

//Strategy for registering BTC addresses
@Component
class BtcRegistrationStrategyImpl(
    @Autowired private val btcAddressesProvider: BtcAddressesProvider,
    @Autowired private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider,
    @Autowired private val irohaAccountCreator: IrohaAccountCreator
) : RegistrationStrategy {

    /**
     * Registers new Iroha client and associates BTC address to it
     * @param name - client name
     * @param pubkey - client public key
     * @param whitelist - list of bitcoin addresses
     * @return associated BTC address
     */
    override fun register(name: String, whitelist: List<String>, pubkey: String): Result<String, Exception> {
        return btcAddressesProvider.getAddresses().fanout { btcRegisteredAddressesProvider.getRegisteredAddresses() }
            .flatMap { (addresses, takenAddresses) ->
                try {
                    //It fetches all BTC addresses and takes one that was not registered
                    val freeAddress = addresses.keys.first { btcAddress -> !takenAddresses.containsKey(btcAddress) }
                    irohaAccountCreator.create(freeAddress, whitelist.toString().trim('[').trim(']'), name, pubkey)
                } catch (e: NoSuchElementException) {
                    throw IllegalStateException("no free btc address to register")
                }
            }
    }
}
