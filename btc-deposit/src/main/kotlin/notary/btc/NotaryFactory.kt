package notary.btc

import io.reactivex.Observable
import notary.NotaryImpl
import notary.btc.config.BtcNotaryConfig
import provider.NotaryPeerListProvider
import sidechain.SideChainEvent

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
