package notary

import io.reactivex.Observable
import sidechain.SideChainEvent

fun createEthNotary(
    notaryConfig: NotaryConfig,
    ethEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>
): NotaryImpl {
    return NotaryImpl(notaryConfig, ethEvents, "ethereum")
}

fun createBtcNotary(
    notaryConfig: NotaryConfig,
    btcEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>
): NotaryImpl {
    return NotaryImpl(notaryConfig, btcEvents, "bitcoin")
}
