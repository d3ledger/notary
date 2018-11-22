package notary

import io.reactivex.Observable
import model.IrohaCredential
import provider.NotaryPeerListProvider
import sidechain.SideChainEvent
import sidechain.iroha.consumer.IrohaNetwork

fun createEthNotary(
    notaryCredential: IrohaCredential,
    irohaNetwork: IrohaNetwork,
    ethEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>,
    peerListProvider: NotaryPeerListProvider
): NotaryImpl {
    return NotaryImpl(
        notaryCredential,
        irohaNetwork,
        ethEvents,
        "ethereum",
        peerListProvider
    )
}
