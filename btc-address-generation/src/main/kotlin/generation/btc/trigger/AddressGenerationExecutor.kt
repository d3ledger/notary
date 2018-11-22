package generation.btc.trigger

import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import generation.btc.config.BtcAddressGenerationConfig
import model.IrohaCredential
import mu.KLogging
import provider.TriggerProvider
import provider.btc.BtcSessionProvider
import provider.btc.address.BtcAddressType
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil

private val logger = KLogging().logger

//TODO springify this file
/**
 * Starts address generation process
 * @param btcPkAddressGenerationConfig - address generation configuration object
 * @param addressType - type of address to generate
 */
fun startAddressGeneration(btcPkAddressGenerationConfig: BtcAddressGenerationConfig, addressType: BtcAddressType) {
    logger.info { "Run BTC multisignature ${addressType.title} address generation" }
    IrohaInitialization.loadIrohaLibrary()
        .flatMap {
            ModelUtil.loadKeypair(
                btcPkAddressGenerationConfig.registrationAccount.pubkeyPath,
                btcPkAddressGenerationConfig.registrationAccount.privkeyPath
            )
        }.map { keypair ->
            IrohaCredential(btcPkAddressGenerationConfig.registrationAccount.accountId, keypair)
        }
        .flatMap { credential ->
            IrohaNetworkImpl(
                btcPkAddressGenerationConfig.iroha.hostname,
                btcPkAddressGenerationConfig.iroha.port
            ).use { irohaNetwork ->
                val sessionAccountName = addressType.createSessionAccountName()
                createBtcSessionProvider(irohaNetwork, credential)
                    .createPubKeyCreationSession(sessionAccountName)
                    .map {
                        createTriggerProvider(btcPkAddressGenerationConfig, irohaNetwork, credential).trigger(
                            sessionAccountName
                        )
                    }
            }
        }.fold(
            { logger.info { "BTC multisignature address generation service was successfully triggered" } },
            { ex ->
                logger.error("Cannot call btc address generation service", ex)
                System.exit(1)
            })
}

//Creates session provider
private fun createBtcSessionProvider(
    irohaNetwork: IrohaNetwork,
    credential: IrohaCredential
) = BtcSessionProvider(credential, irohaNetwork)

//Creates trigger provider
private fun createTriggerProvider(
    btcPkAddressGenerationConfig: BtcAddressGenerationConfig,
    irohaNetwork: IrohaNetwork,
    credential: IrohaCredential
) = TriggerProvider(credential, irohaNetwork, btcPkAddressGenerationConfig.pubKeyTriggerAccount)
