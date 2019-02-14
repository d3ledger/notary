package integration.btc.environment

import com.d3.btc.handler.NewBtcClientRegistrationHandler
import com.d3.btc.listener.NewBtcClientRegistrationListener
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.provider.network.BtcRegTestConfigProvider
import config.BitcoinConfig
import integration.helper.BtcIntegrationHelperUtil
import model.IrohaCredential
import notary.btc.init.BtcNotaryInitialization
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.util.ModelUtil
import java.io.Closeable
import java.io.File
import java.net.InetAddress


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

    private val peerGroup by lazy {
        val peerGroup = integrationHelper.getPeerGroup(
            wallet,
            btcNetworkConfigProvider,
            notaryConfig.bitcoin.blockStoragePath
        )
        BitcoinConfig.extractHosts(notaryConfig.bitcoin).forEach { host ->
            peerGroup.addAddress(InetAddress.getByName(host))
        }
        peerGroup
    }
    val btcNotaryInitialization =
        BtcNotaryInitialization(
            peerGroup,
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
