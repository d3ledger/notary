package com.d3.btc.deposit.factory

import io.reactivex.Observable
import jp.co.soramitsu.iroha.java.IrohaAPI
import model.IrohaCredential
import notary.NotaryImpl
import provider.NotaryPeerListProvider
import sidechain.SideChainEvent

fun createBtcNotary(
    notaryCredential: IrohaCredential,
    irohaAPI: IrohaAPI,
    btcEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>,
    peerListProvider: NotaryPeerListProvider
): NotaryImpl {
    return NotaryImpl(
        notaryCredential,
        irohaAPI,
        btcEvents,
        "bitcoin",
        peerListProvider
    )
}
