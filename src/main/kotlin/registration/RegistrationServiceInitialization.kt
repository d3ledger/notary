package registration

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import main.ConfigKeys
import notary.CONFIG
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaKeyLoader

/**
 * Initialisation of Registration Service
 */
class RegistrationServiceInitialization {

    /**
     * Init Registration Service
     */
    fun init(): Result<Unit, Exception> {
        return Result.of {
            IrohaKeyLoader.loadKeypair(CONFIG[ConfigKeys.pubkeyPath], CONFIG[ConfigKeys.privkeyPath])
                .map { Pair(EthFreeWalletsProvider(it), IrohaConsumerImpl(it)) }
                .map { (ethFreeWalletsProvider, irohaConsumer) ->
                    RegistrationStrategyImpl(ethFreeWalletsProvider, irohaConsumer)
                }
                .map { RegistrationServiceEndpoint(CONFIG[ConfigKeys.registrationPort], it) }
            Unit
        }
    }

}
