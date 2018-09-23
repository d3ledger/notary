package notary

import io.reactivex.Observable
import model.IrohaCredential
import notary.btc.BtcNotaryConfig
import notary.eth.EthNotaryConfig
import sidechain.SideChainEvent
import sidechain.iroha.util.ModelUtil

fun createEthNotary(
    ethNotaryConfig: EthNotaryConfig,
    ethEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>
): NotaryImpl {
    val credential = IrohaCredential(
        ethNotaryConfig.notaryCredential.accountId,
        ModelUtil.loadKeypair(
            ethNotaryConfig.notaryCredential.pubkeyPath,
            ethNotaryConfig.notaryCredential.privkeyPath
        ).get()
    )
    return NotaryImpl(
        ethNotaryConfig.iroha,
        credential,
        ethEvents,
        "ethereum",
        ethNotaryConfig.notaryListStorageAccount,
        ethNotaryConfig.notaryListSetterAccount
    )
}

fun createBtcNotary(
    btcNotaryConfig: BtcNotaryConfig,
    btcEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>
): NotaryImpl {
    val credential = IrohaCredential(
        btcNotaryConfig.notaryCredential.accountId,
        ModelUtil.loadKeypair(
            btcNotaryConfig.notaryCredential.pubkeyPath,
            btcNotaryConfig.notaryCredential.privkeyPath
        ).get()
    )
    return NotaryImpl(
        btcNotaryConfig.iroha,
        credential,
        btcEvents,
        "bitcoin",
        btcNotaryConfig.notaryListStorageAccount,
        btcNotaryConfig.notaryListSetterAccount
    )
}
