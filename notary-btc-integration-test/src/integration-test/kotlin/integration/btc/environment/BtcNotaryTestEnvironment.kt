package integration.btc.environment

import handler.btc.NewBtcClientRegistrationHandler
import integration.helper.BtcIntegrationHelperUtil
import listener.btc.NewBtcClientRegistrationListener
import model.IrohaCredential
import notary.btc.BtcNotaryInitialization
import provider.btc.address.BtcRegisteredAddressesProvider
import provider.btc.network.BtcRegTestConfigProvider
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.util.ModelUtil
import java.io.Closeable
import java.io.File


/**
 * Bitcoin notary service testing environment
 */
class BtcNotaryTestEnvironment(private val integrationHelper: BtcIntegrationHelperUtil, testName: String = "") :
    Closeable {

    val notaryConfig = integrationHelper.configHelper.createBtcNotaryConfig(testName)

    private val irohaCredential = IrohaCredential(
        notaryConfig.notaryCredential.accountId,
        ModelUtil.loadKeypair(notaryConfig.notaryCredential.pubkeyPath, notaryConfig.notaryCredential.privkeyPath).get()
    )

    private val btcRegisteredAddressesProvider = BtcRegisteredAddressesProvider(
        integrationHelper.queryAPI,
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

    private val wallet = org.bitcoinj.wallet.Wallet.loadFromFile(File(notaryConfig.bitcoin.walletPath))

    val btcNotaryInitialization =
        BtcNotaryInitialization(
            wallet,
            notaryConfig,
            irohaCredential,
            integrationHelper.irohaAPI,
            btcRegisteredAddressesProvider,
            irohaChainListener,
            newBtcClientRegistrationListener,
            btcNetworkConfigProvider
        )

    override fun close() {
        integrationHelper.close()
        irohaChainListener.close()
        //Clear bitcoin blockchain folder
        File(notaryConfig.bitcoin.blockStoragePath).deleteRecursively()
        btcNotaryInitialization.close()
    }
}
