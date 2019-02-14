package integration.btc.environment

import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.provider.account.IrohaBtcAccountCreator
import com.d3.btc.provider.address.BtcAddressesProvider
import integration.helper.BtcIntegrationHelperUtil
import model.IrohaCredential
import registration.btc.init.BtcRegistrationServiceInitialization
import registration.btc.strategy.BtcRegistrationStrategyImpl
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
        BtcRegistrationStrategyImpl(
            btcAddressesProvider(),
            btcRegisteredAddressesProvider(),
            irohaBtcAccountCreator()
        )
    )

    private fun btcAddressesProvider(): BtcAddressesProvider {
        return BtcAddressesProvider(
            integrationHelper.queryAPI,
            btcRegistrationConfig.mstRegistrationAccount,
            btcRegistrationConfig.notaryAccount
        )
    }

    private fun btcRegisteredAddressesProvider(): BtcRegisteredAddressesProvider {
        return BtcRegisteredAddressesProvider(
            integrationHelper.queryAPI,
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
        integrationHelper.queryAPI,
        btcRegistrationConfig.registrationCredential.accountId,
        integrationHelper.accountHelper.notaryAccount.accountId
    )

    override fun close() {
        integrationHelper.close()
    }
}
