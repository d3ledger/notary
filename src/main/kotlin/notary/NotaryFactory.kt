package notary

import io.reactivex.Observable
import model.IrohaCredential
import notary.eth.EthNotaryConfig
import provider.NotaryPeerListProvider
import sidechain.SideChainEvent
import sidechain.iroha.util.ModelUtil

fun createEthNotary(
    ethNotaryConfig: EthNotaryConfig,
    ethEvents: Observable<SideChainEvent.PrimaryBlockChainEvent>,
    peerListProvider: NotaryPeerListProvider
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
        peerListProvider
    )
}
