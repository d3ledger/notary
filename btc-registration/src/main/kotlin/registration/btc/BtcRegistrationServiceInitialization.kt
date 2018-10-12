package registration.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import model.IrohaCredential
import mu.KLogging
import provider.btc.BtcAddressesProvider
import provider.btc.BtcRegisteredAddressesProvider
import registration.RegistrationServiceEndpoint
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl

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
            val irohaNetwork = IrohaNetworkImpl(btcRegistrationConfig.iroha.hostname, btcRegistrationConfig.iroha.port)
            val irohaConsumer = IrohaConsumerImpl(btcRegistrationCredential, irohaNetwork)
            val btcAddressesProvider =
                BtcAddressesProvider(
                    btcRegistrationConfig.iroha,
                    btcRegistrationCredential,
                    btcRegistrationConfig.mstRegistrationAccount,
                    btcRegistrationConfig.notaryAccount
                )
            val btcTakenAddressesProvider =
                BtcRegisteredAddressesProvider(
                    btcRegistrationConfig.iroha,
                    btcRegistrationCredential,
                    btcRegistrationCredential.accountId,
                    btcRegistrationConfig.notaryAccount
                )
            BtcRegistrationStrategyImpl(
                btcAddressesProvider,
                btcTakenAddressesProvider,
                irohaConsumer,
                btcRegistrationConfig.notaryAccount
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

