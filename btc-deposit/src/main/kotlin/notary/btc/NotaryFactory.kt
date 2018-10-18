package notary.btc

import io.reactivex.Observable
import model.IrohaCredential
import notary.NotaryImpl
import provider.NotaryPeerListProvider
import sidechain.SideChainEvent
import sidechain.iroha.consumer.IrohaNetwork

fun createBtcNotary(
    notaryCredential: IrohaCredential,
    irohaNetwork: IrohaNetwork,
    btcEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>,
    peerListProvider: NotaryPeerListProvider
): NotaryImpl {
    return NotaryImpl(
        notaryCredential,
        irohaNetwork,
        btcEvents,
        "bitcoin",
        peerListProvider
    )
}
