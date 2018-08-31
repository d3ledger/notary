package registration.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import provider.eth.EthFreeRelayProvider
import registration.IrohaAccountCreator
import registration.RegistrationStrategy
import sidechain.iroha.consumer.IrohaConsumer

/**
 * Effective implementation of [RegistrationStrategy]
 */
class EthRegistrationStrategyImpl(
    private val ethFreeRelayProvider: EthFreeRelayProvider,
    irohaConsumer: IrohaConsumer,
    notaryIrohaAccount: String,
    creator: String
) : RegistrationStrategy {
    private val irohaAccountCreator = IrohaAccountCreator(irohaConsumer, notaryIrohaAccount, creator, "ethereum_wallet")
    /**
     * Register new notary client
     * @param name - client name
     * @param pubkey - client public key
     * @return ethereum wallet has been registered
     */
    override fun register(name: String, pubkey: String): Result<String, Exception> {
        return ethFreeRelayProvider.getRelay()
            .flatMap { freeEthWallet ->
                irohaAccountCreator.create(freeEthWallet, name, pubkey)
            }
    }


}
