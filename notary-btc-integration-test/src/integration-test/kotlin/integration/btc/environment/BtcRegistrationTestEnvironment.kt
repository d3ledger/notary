package integration.btc.environment

import com.d3.btc.provider.BtcFreeAddressesProvider
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.provider.account.IrohaBtcAccountRegistrator
import com.d3.btc.provider.address.BtcAddressesProvider
import com.d3.btc.registration.init.BtcRegistrationServiceInitialization
import com.d3.btc.registration.strategy.BtcRegistrationStrategyImpl
import integration.helper.BtcIntegrationHelperUtil
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import java.io.Closeable

/**
 * Bitcoin client registration service testing environment
 */
class BtcRegistrationTestEnvironment(private val integrationHelper: BtcIntegrationHelperUtil) : Closeable {

    val btcRegistrationConfig = integrationHelper.configHelper.createBtcRegistrationConfig()

    val btcAddressGenerationConfig = integrationHelper.configHelper.createBtcAddressGenerationConfig(0)

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
            BtcFreeAddressesProvider(
                btcRegistrationConfig.nodeId, btcAddressesProvider(),
                btcRegisteredAddressesProvider()
            ),
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

    private fun irohaBtcAccountCreator(): IrohaBtcAccountRegistrator {
        return IrohaBtcAccountRegistrator(
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
