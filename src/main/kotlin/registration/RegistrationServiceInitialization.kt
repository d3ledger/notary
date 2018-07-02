package registration

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import main.ConfigKeys
import mu.KLogging
import notary.CONFIG
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil

/**
 * Initialisation of Registration Service
 */
class RegistrationServiceInitialization {

    /**
     * Init Registration Service
     */
    fun init(): Result<Unit, Exception> {
        return Result.of {
            // init iroha listener
            initIrohaConsumer()
                .map { initRegistrationStrategy(it) }
                .map { initHttpEndpoint(it) }
            Unit
        }
    }

    /**
     * Initialize Iroha consumer to write to chain
     */
    private fun initIrohaConsumer(): Result<IrohaConsumer, Exception> {
        logger.info { "Init Iroha consumer" }
        return ModelUtil.loadKeypair(CONFIG[ConfigKeys.pubkeyPath], CONFIG[ConfigKeys.privkeyPath])
            .map {
                IrohaConsumerImpl(it)
            }
    }

    /**
     * Initialize registration strategy in Iroha chain
     */
    private fun initRegistrationStrategy(irohaConsumer: IrohaConsumer): RegistrationStrategy {
        return RegistrationStrategyImpl(EthFreeWalletsProvider(), irohaConsumer)
    }

    /**
     * Init Registration Service endpoint
     */
    private fun initHttpEndpoint(strategy: RegistrationStrategy) {
        RegistrationServiceEndpoint(CONFIG[ConfigKeys.registrationPort], strategy)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
