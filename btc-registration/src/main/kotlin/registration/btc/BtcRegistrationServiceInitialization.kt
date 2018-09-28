package registration.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import model.IrohaCredential
import mu.KLogging
import provider.btc.BtcAddressesProvider
import provider.btc.BtcRegisteredAddressesProvider
import registration.RegistrationServiceEndpoint
import sidechain.iroha.consumer.IrohaConsumerImpl

class BtcRegistrationServiceInitialization(
    private val btcRegistrationConfig: BtcRegistrationConfig,
    private val btcRegistrationCredential: IrohaCredential
) {
    /**
     * Init Registration Service
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Init BTC client registration service" }
        return Result.of {
            val irohaConsumer = IrohaConsumerImpl(btcRegistrationCredential, btcRegistrationConfig.iroha)
            val btcAddressesProvider =
                BtcAddressesProvider(
                    btcRegistrationConfig.iroha,
                    btcRegistrationCredential,
                    btcRegistrationConfig.mstRegistrationAccount,
                    btcRegistrationCredential.accountId
                )
            val btcTakenAddressesProvider =
                BtcRegisteredAddressesProvider(
                    btcRegistrationConfig.iroha,
                    btcRegistrationCredential,
                    btcRegistrationCredential.accountId,
                    btcRegistrationCredential.accountId
                )
            BtcRegistrationStrategyImpl(
                btcAddressesProvider,
                btcTakenAddressesProvider,
                irohaConsumer,
                btcRegistrationCredential.accountId
            )
        }.map { registrationStrategy ->
            RegistrationServiceEndpoint(
                btcRegistrationConfig.port,
                registrationStrategy
            )
            Unit
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}

