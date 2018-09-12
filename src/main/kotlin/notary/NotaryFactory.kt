package notary

import io.reactivex.Observable
import notary.btc.BtcNotaryConfig
import notary.eth.EthNotaryConfig
import sidechain.SideChainEvent

fun createEthNotary(
    ethNotaryConfig: EthNotaryConfig,
    ethEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>
): NotaryImpl {
    return NotaryImpl(
        ethNotaryConfig.iroha,
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
    return NotaryImpl(
        btcNotaryConfig.iroha,
        btcEvents,
        "bitcoin",
        btcNotaryConfig.notaryListStorageAccount,
        btcNotaryConfig.notaryListSetterAccount
    )
}
