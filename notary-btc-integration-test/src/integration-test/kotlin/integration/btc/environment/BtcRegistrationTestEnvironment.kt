package integration.btc.environment

import integration.helper.BtcIntegrationHelperUtil
import model.IrohaCredential
import provider.btc.account.IrohaBtcAccountCreator
import provider.btc.address.BtcAddressesProvider
import provider.btc.address.BtcRegisteredAddressesProvider
import registration.btc.BtcRegistrationServiceInitialization
import registration.btc.BtcRegistrationStrategyImpl
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.util.ModelUtil
import java.io.Closeable

/**
 * Bitcoin client registration service testing environment
 */
class BtcRegistrationTestEnvironment(private val integrationHelper: BtcIntegrationHelperUtil) : Closeable {

    val btcRegistrationConfig = integrationHelper.configHelper.createBtcRegistrationConfig()

    val btcNotaryConfig = integrationHelper.configHelper.createBtcNotaryConfig()

    private val btcRegistrationCredential = ModelUtil.loadKeypair(
        btcRegistrationConfig.registrationCredential.pubkeyPath,
        btcRegistrationConfig.registrationCredential.privkeyPath
    ).fold(
        { keypair ->
            IrohaCredential(btcRegistrationConfig.registrationCredential.accountId, keypair)
        },
        { ex -> throw ex }
    )

    private val btcClientCreatorConsumer = IrohaConsumerImpl(btcRegistrationCredential, integrationHelper.irohaAPI)

    val btcRegistrationServiceInitialization = BtcRegistrationServiceInitialization(
        btcRegistrationConfig,
        BtcRegistrationStrategyImpl(btcAddressesProvider(), btcRegisteredAddressesProvider(), irohaBtcAccountCreator())
    )

    private fun btcAddressesProvider(): BtcAddressesProvider {
        return BtcAddressesProvider(
            btcRegistrationCredential,
            integrationHelper.irohaAPI,
            btcRegistrationConfig.mstRegistrationAccount,
            btcRegistrationConfig.notaryAccount
        )
    }

    private fun btcRegisteredAddressesProvider(): BtcRegisteredAddressesProvider {
        return BtcRegisteredAddressesProvider(
            btcRegistrationCredential,
            integrationHelper.irohaAPI,
            btcRegistrationCredential.accountId,
            btcRegistrationConfig.notaryAccount
        )
    }

    private fun irohaBtcAccountCreator(): IrohaBtcAccountCreator {
        return IrohaBtcAccountCreator(
            btcClientCreatorConsumer,
            btcRegistrationConfig.notaryAccount
        )
    }

    val btcTakenAddressesProvider = BtcRegisteredAddressesProvider(
        integrationHelper.testCredential,
        integrationHelper.irohaAPI,
        btcRegistrationConfig.registrationCredential.accountId,
        integrationHelper.accountHelper.notaryAccount.accountId
    )

    override fun close() {
        integrationHelper.close()
    }
}
