package notary

import io.reactivex.Observable
import notary.btc.BtcNotaryConfig
import notary.eth.EthNotaryConfig
import provider.NotaryPeerListProvider
import sidechain.SideChainEvent

fun createEthNotary(
    ethNotaryConfig: EthNotaryConfig,
    ethEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>,
    peerListProvider: NotaryPeerListProvider
): NotaryImpl {
    return NotaryImpl(
        ethNotaryConfig.iroha,
        ethEvents,
        "ethereum",
        peerListProvider
    )
}

fun createBtcNotary(
    btcNotaryConfig: BtcNotaryConfig,
    btcEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>,
    peerListProvider: NotaryPeerListProvider
): NotaryImpl {
    return NotaryImpl(
        btcNotaryConfig.iroha,
        btcEvents,
        "bitcoin",
        peerListProvider
    )
}
