package integration.btc.environment

import com.d3.btc.deposit.config.BtcDepositConfig
import com.d3.btc.deposit.init.BtcNotaryInitialization
import com.d3.btc.handler.NewBtcClientRegistrationHandler
import com.d3.btc.listener.NewBtcClientRegistrationListener
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.provider.network.BtcRegTestConfigProvider
import com.d3.commons.config.BitcoinConfig
import integration.helper.BtcIntegrationHelperUtil
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.IrohaChainListener
import com.d3.commons.sidechain.iroha.util.ModelUtil
import java.io.Closeable
import java.io.File


/**
 * Bitcoin notary service testing environment
 */
class BtcNotaryTestEnvironment(
    private val integrationHelper: BtcIntegrationHelperUtil,
    testName: String = "",
    val notaryConfig: BtcDepositConfig = integrationHelper.configHelper.createBtcDepositConfig(testName),
    notaryCredential: IrohaCredential = IrohaCredential(
        notaryConfig.notaryCredential.accountId,
        ModelUtil.loadKeypair(notaryConfig.notaryCredential.pubkeyPath, notaryConfig.notaryCredential.privkeyPath).get()
    )
) : Closeable {

    private val btcRegisteredAddressesProvider = BtcRegisteredAddressesProvider(
        integrationHelper.queryAPI,
        notaryConfig.registrationAccount,
        notaryConfig.notaryCredential.accountId
    )

    val btcAddressGenerationConfig = integrationHelper.configHelper.createBtcAddressGenerationConfig(0)

    private val btcNetworkConfigProvider = BtcRegTestConfigProvider()

    val irohaChainListener = IrohaChainListener(
        notaryConfig.iroha.hostname,
        notaryConfig.iroha.port,
        notaryCredential
    )

    private val newBtcClientRegistrationListener =
        NewBtcClientRegistrationListener(NewBtcClientRegistrationHandler(btcNetworkConfigProvider))

    private val transferWallet = org.bitcoinj.wallet.Wallet.loadFromFile(File(notaryConfig.btcTransferWalletPath))

    private val peerGroup = integrationHelper.getPeerGroup(
        transferWallet,
        btcNetworkConfigProvider,
        notaryConfig.bitcoin.blockStoragePath,
        BitcoinConfig.extractHosts(notaryConfig.bitcoin)
    )

    val btcNotaryInitialization =
        BtcNotaryInitialization(
            peerGroup,
            transferWallet,
            notaryConfig,
            notaryCredential,
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
