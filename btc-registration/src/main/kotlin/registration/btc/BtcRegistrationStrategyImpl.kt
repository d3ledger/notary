package registration.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.flatMap
import provider.btc.BtcAddressesProvider
import provider.btc.BtcRegisteredAddressesProvider
import registration.IrohaAccountCreator
import registration.RegistrationStrategy
import sidechain.iroha.consumer.IrohaConsumer

//Strategy for registering BTC addresses
class BtcRegistrationStrategyImpl(
    private val btcAddressesProvider: BtcAddressesProvider,
    private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider,
    irohaConsumer: IrohaConsumer,
    notaryIrohaAccount: String
) : RegistrationStrategy {

    private val irohaAccountCreator =
        IrohaAccountCreator(irohaConsumer, notaryIrohaAccount, "bitcoin")
    
    /**
     * Registers new Iroha client and associates BTC address to it
     * @param name - client name
     * @param pubkey - client public key
     * @return associated BTC address
     */
    override fun register(name: String, pubkey: String): Result<String, Exception> {
        return btcAddressesProvider.getAddresses().fanout { btcRegisteredAddressesProvider.getRegisteredAddresses() }
            .flatMap { (addresses, takenAddresses) ->
                try {
                    //It fetches all BTC addresses and takes one that was not registered
                    val freeAddress = addresses.keys.first { btcAddress -> !takenAddresses.containsKey(btcAddress) }
                    irohaAccountCreator.create(freeAddress, name, pubkey)
                } catch (e: NoSuchElementException) {
                    throw IllegalStateException("no free btc address to register")
                }
            }
    }
}
