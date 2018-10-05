package notary.btc

import io.reactivex.Observable
import model.IrohaCredential
import notary.NotaryImpl
import notary.btc.config.BtcNotaryConfig
import provider.NotaryPeerListProvider
import sidechain.SideChainEvent

fun createBtcNotary(
    btcNotaryConfig: BtcNotaryConfig,
    notaryCredential: IrohaCredential,
    btcEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>,
    peerListProvider: NotaryPeerListProvider
): NotaryImpl {


    return NotaryImpl(
        btcNotaryConfig.iroha,
        notaryCredential,
        btcEvents,
        "bitcoin",
        peerListProvider
    )
}
