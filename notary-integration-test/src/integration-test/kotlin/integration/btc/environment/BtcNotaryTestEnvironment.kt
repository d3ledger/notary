package integration.btc.environment

import handler.btc.NewBtcClientRegistrationHandler
import integration.helper.IntegrationHelperUtil
import listener.btc.NewBtcClientRegistrationListener
import model.IrohaCredential
import notary.btc.BtcNotaryInitialization
import provider.btc.address.BtcRegisteredAddressesProvider
import provider.btc.network.BtcRegTestConfigProvider
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.util.ModelUtil


/**
 * Bitcoin notary service testing environment
 */
class BtcNotaryTestEnvironment(integrationHelper: IntegrationHelperUtil) {

    val notaryConfig = integrationHelper.configHelper.createBtcNotaryConfig()

    private val irohaCredential = IrohaCredential(
        notaryConfig.notaryCredential.accountId,
        ModelUtil.loadKeypair(notaryConfig.notaryCredential.pubkeyPath, notaryConfig.notaryCredential.privkeyPath).get()
    )

    private val btcRegisteredAddressesProvider = BtcRegisteredAddressesProvider(
        irohaCredential,
        integrationHelper.irohaNetwork,
        notaryConfig.registrationAccount,
        notaryConfig.notaryCredential.accountId
    )

    private val btcNetworkConfigProvider = BtcRegTestConfigProvider()

    val irohaChainListener = IrohaChainListener(
        notaryConfig.iroha.hostname,
        notaryConfig.iroha.port,
        irohaCredential
    )

    private val newBtcClientRegistrationListener =
        NewBtcClientRegistrationListener(NewBtcClientRegistrationHandler(btcNetworkConfigProvider))


    val btcNotaryInitialization =
        BtcNotaryInitialization(
            notaryConfig,
            irohaCredential,
            integrationHelper.irohaNetwork,
            btcRegisteredAddressesProvider,
            irohaChainListener,
            newBtcClientRegistrationListener,
            btcNetworkConfigProvider
        )
}
