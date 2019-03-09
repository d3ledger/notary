package com.d3.btc.listener

import com.d3.btc.handler.NewBtcClientRegistrationHandler
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import iroha.protocol.BlockOuterClass
import mu.KLogging
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.d3.commons.sidechain.iroha.util.getSetDetailCommands
import java.util.concurrent.Executors

/**
 * Class that is used to listen to new client registration events
 */
@Component
class NewBtcClientRegistrationListener(
    @Autowired private val newBtcClientRegistrationHandler: NewBtcClientRegistrationHandler
) {
    /**
     * Listens to newly registered Bitcoin addresses and adds addresses to current wallet object
     */
    fun listenToRegisteredClients(
        wallet: Wallet,
        irohaObservable: Observable<BlockOuterClass.Block>,
        onChainListenerFail: () -> Unit
    ) {
        irohaObservable.subscribeOn(Schedulers.from(Executors.newSingleThreadExecutor()))
            .subscribe({ block ->
                getSetDetailCommands(block).forEach { command ->
                    newBtcClientRegistrationHandler.handleNewClientCommand(command, wallet)
                }
            }, { ex ->
                logger.error("Error on subscribe", ex)
                onChainListenerFail()
            })
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
