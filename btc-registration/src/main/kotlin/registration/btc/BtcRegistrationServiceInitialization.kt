package registration.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.Keypair
import mu.KLogging
import provider.btc.BtcAddressesProvider
import provider.btc.BtcRegisteredAddressesProvider
import registration.RegistrationServiceEndpoint
import sidechain.iroha.consumer.IrohaConsumerImpl

class BtcRegistrationServiceInitialization(
    private val btcRegistrationConfig: BtcRegistrationConfig,
    private val keyPair: Keypair
) {
    /**
     * Init Registration Service
     */
    fun init(): Result<Unit, Exception> {
        logger.info { "Init BTC client registration service" }
        return Result.of {
            val irohaConsumer = IrohaConsumerImpl(btcRegistrationConfig.iroha.creator, btcRegistrationConfig.iroha)
            val btcAddressesProvider =
                BtcAddressesProvider(
                    btcRegistrationConfig.iroha,
                    keyPair,
                    btcRegistrationConfig.mstRegistrationAccount,
                    btcRegistrationConfig.iroha.creator
                )
            val btcTakenAddressesProvider =
                BtcRegisteredAddressesProvider(
                    btcRegistrationConfig.iroha,
                    keyPair,
                    btcRegistrationConfig.registrationAccount,
                    btcRegistrationConfig.iroha.creator
                )
            BtcRegistrationStrategyImpl(
                btcAddressesProvider,
                btcTakenAddressesProvider,
                irohaConsumer,
                btcRegistrationConfig.iroha.creator,
                btcRegistrationConfig.registrationAccount
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

