package registration.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.flatMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import provider.btc.account.IrohaBtcAccountCreator
import provider.btc.address.BtcAddressesProvider
import provider.btc.address.BtcRegisteredAddressesProvider
import registration.RegistrationStrategy

//Strategy for registering BTC addresses
@Component
class BtcRegistrationStrategyImpl(
    @Autowired private val btcAddressesProvider: BtcAddressesProvider,
    @Autowired private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider,
    @Autowired private val irohaBtcAccountCreator: IrohaBtcAccountCreator
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
                    //TODO Warning. Race condition ahead. Multiple threads/nodes can register the same BTC address twice.
                    //It fetches all BTC addresses and takes one that was not registered
                    val freeAddress =
                        addresses.first { btcAddress -> !takenAddresses.any { takenAddress -> takenAddress.address == btcAddress.address } }
                    irohaBtcAccountCreator.create(
                        freeAddress.address,
                        whitelist,
                        name,
                        pubkey,
                        freeAddress.info.notaryKeys
                    )
                } catch (e: NoSuchElementException) {
                    throw IllegalStateException("no free btc address to register")
                }
            }
    }
}
