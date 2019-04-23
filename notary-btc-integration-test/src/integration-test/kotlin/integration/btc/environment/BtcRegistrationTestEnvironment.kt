package integration.btc.environment

import com.d3.btc.provider.BtcFreeAddressesProvider
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.provider.account.IrohaBtcAccountRegistrator
import com.d3.btc.provider.address.BtcAddressesProvider
import com.d3.btc.registration.init.BtcRegistrationServiceInitialization
import com.d3.btc.registration.strategy.BtcRegistrationStrategyImpl
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.toHexString
import integration.helper.BtcIntegrationHelperUtil
import khttp.post
import khttp.responses.Response
import java.io.Closeable

/**
 * Bitcoin client registration service testing environment
 */
class BtcRegistrationTestEnvironment(private val integrationHelper: BtcIntegrationHelperUtil) :
    Closeable {

    val btcRegistrationConfig = integrationHelper.configHelper.createBtcRegistrationConfig()

    val btcAddressGenerationConfig =
        integrationHelper.configHelper.createBtcAddressGenerationConfig(0)

    private val btcRegistrationCredential = ModelUtil.loadKeypair(
        btcRegistrationConfig.registrationCredential.pubkeyPath,
        btcRegistrationConfig.registrationCredential.privkeyPath
    ).fold(
        { keypair ->
            IrohaCredential(btcRegistrationConfig.registrationCredential.accountId, keypair)
        },
        { ex -> throw ex }
    )

    private val btcClientCreatorConsumer =
        IrohaConsumerImpl(btcRegistrationCredential, integrationHelper.irohaAPI)

    val btcFreeAddressesProvider = BtcFreeAddressesProvider(
        btcRegistrationConfig.nodeId, btcAddressesProvider(),
        btcRegisteredAddressesProvider()
    )

    val btcRegistrationServiceInitialization = BtcRegistrationServiceInitialization(
        btcRegistrationConfig,
        BtcRegistrationStrategyImpl(
            btcRegisteredAddressesProvider(),
            btcFreeAddressesProvider,
            irohaBtcAccountCreator()
        )
    )

    private fun btcAddressesProvider(): BtcAddressesProvider {
        return BtcAddressesProvider(
            integrationHelper.queryHelper,
            btcRegistrationConfig.mstRegistrationAccount,
            btcRegistrationConfig.notaryAccount
        )
    }

    private fun btcRegisteredAddressesProvider(): BtcRegisteredAddressesProvider {
        return BtcRegisteredAddressesProvider(
            integrationHelper.queryHelper,
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

    val btcRegisteredAddressesProvider = BtcRegisteredAddressesProvider(
        integrationHelper.queryHelper,
        btcRegistrationConfig.registrationCredential.accountId,
        integrationHelper.accountHelper.notaryAccount.accountId
    )

    fun register(
        name: String,
        pubkey: String = ModelUtil.generateKeypair().public.toHexString()
    ): Response {
        return post(
            "http://127.0.0.1:${btcRegistrationConfig.port}/users",
            data = mapOf("name" to name, "pubkey" to pubkey)
        )
    }

    override fun close() {
        integrationHelper.close()
    }
}
