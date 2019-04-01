package com.d3.btc.listener

import com.d3.btc.handler.NewBtcClientRegistrationHandler
import com.d3.commons.sidechain.iroha.util.getSetDetailCommands
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import iroha.protocol.BlockOuterClass
import mu.KLogging
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutorService

/**
 * Class that is used to listen to new client registration events
 */
@Component
class NewBtcClientRegistrationListener(
    @Autowired private val newBtcClientRegistrationHandler: NewBtcClientRegistrationHandler,
    @Qualifier("registeredClientsListenerExecutor")
    @Autowired private val registeredClientsListenerExecutor: ExecutorService
) {
    /**
     * Listens to newly registered Bitcoin addresses and adds addresses to current wallet object
     */
    fun listenToRegisteredClients(
        wallet: Wallet,
        irohaObservable: Observable<BlockOuterClass.Block>,
        onChainListenerFail: () -> Unit
    ) {
        irohaObservable.subscribeOn(
            Schedulers.from(registeredClientsListenerExecutor)
        ).subscribe({ block ->
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
