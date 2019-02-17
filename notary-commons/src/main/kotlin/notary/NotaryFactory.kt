package notary

import io.reactivex.Observable
import jp.co.soramitsu.iroha.java.IrohaAPI
import model.IrohaCredential
import provider.NotaryPeerListProvider
import sidechain.SideChainEvent

fun createEthNotary(
    notaryCredential: IrohaCredential,
    irohaAPI: IrohaAPI,
    ethEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>,
    peerListProvider: NotaryPeerListProvider
): NotaryImpl {
    return NotaryImpl(
        notaryCredential,
        irohaAPI,
        ethEvents,
        "ethereum",
        peerListProvider
    )
}
