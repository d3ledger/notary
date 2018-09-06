package registration.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.flatMap
import provider.btc.BtcAddressesProvider
import provider.btc.BtcTakenAddressesProvider
import registration.IrohaAccountCreator
import registration.RegistrationStrategy
import sidechain.iroha.consumer.IrohaConsumer

class BtcRegistrationStrategyImpl(
    private val btcAddressesProvider: BtcAddressesProvider,
    private val btcTakenAddressesProvider: BtcTakenAddressesProvider,
    irohaConsumer: IrohaConsumer,
    notaryIrohaAccount: String,
    registrationAccount: String
) : RegistrationStrategy {

    private val irohaAccountCreator =
        IrohaAccountCreator(irohaConsumer, notaryIrohaAccount, registrationAccount, "bitcoin")

    override fun register(name: String, pubkey: String): Result<String, Exception> {
        return btcAddressesProvider.getAddresses().fanout { btcTakenAddressesProvider.getTakenAddresses() }
            .flatMap { (addresses, takenAddresses) ->
                try {
                    val freeAddress = addresses.keys.first { btcAddress -> !takenAddresses.containsKey(btcAddress) }
                    irohaAccountCreator.create(freeAddress, name, pubkey)
                } catch (e: NoSuchElementException) {
                    throw IllegalStateException("no free btc address to register")
                }
            }
    }
}
