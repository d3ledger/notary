package com.d3.btc.generation.init

import com.d3.btc.generation.BTC_ADDRESS_GENERATION_SERVICE_NAME
import com.d3.btc.generation.config.BtcAddressGenerationConfig
import com.d3.btc.generation.trigger.AddressGenerationTrigger
import com.d3.btc.healthcheck.HealthyService
import com.d3.btc.model.BtcAddressType
import com.d3.btc.model.getAddressTypeByAccountId
import com.d3.btc.provider.account.BTC_CURRENCY_NAME_KEY
import com.d3.btc.provider.generation.ADDRESS_GENERATION_NODE_ID_KEY
import com.d3.btc.provider.generation.ADDRESS_GENERATION_TIME_KEY
import com.d3.btc.provider.generation.BtcPublicKeyProvider
import com.d3.btc.provider.network.BtcNetworkConfigProvider
import com.d3.btc.wallet.checkWalletNetwork
import com.d3.btc.wallet.safeSave
import com.d3.commons.sidechain.iroha.CLIENT_DOMAIN
import com.d3.commons.sidechain.iroha.IrohaChainListener
import com.d3.commons.sidechain.iroha.util.getAccountDetails
import com.d3.commons.sidechain.iroha.util.getSetDetailCommands
import com.d3.commons.util.createPrettySingleThreadPool
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import jp.co.soramitsu.iroha.java.QueryAPI
import mu.KLogging
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/*
   This class listens to special account to be triggered and starts generation process
 */
@Component
class BtcAddressGenerationInitialization(
    @Autowired private val keysWallet: Wallet,
    @Qualifier("registrationQueryAPI")
    @Autowired private val registrationQueryAPI: QueryAPI,
    @Autowired private val btcAddressGenerationConfig: BtcAddressGenerationConfig,
    @Autowired private val btcPublicKeyProvider: BtcPublicKeyProvider,
    @Autowired private val irohaChainListener: IrohaChainListener,
    @Autowired private val addressGenerationTrigger: AddressGenerationTrigger,
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider
) : HealthyService() {

    /*
    Initiates listener that listens to events in trigger account.
    If trigger account is triggered, new session account full notary public keys will be created
     */
    fun init(): Result<Unit, Exception> {
        //Check wallet network
        return keysWallet.checkWalletNetwork(btcNetworkConfigProvider.getConfig()).flatMap {
            irohaChainListener.getBlockObservable()
        }.map { irohaObservable ->
            initIrohaObservable(irohaObservable)
        }.flatMap {
            // Start free address generation at initial phase
            addressGenerationTrigger
                .startFreeAddressGenerationIfNeeded(
                    btcAddressGenerationConfig.threshold,
                    btcAddressGenerationConfig.nodeId
                )
        }.flatMap {
            // Start change address generation at initial phase
            addressGenerationTrigger
                .startChangeAddressGenerationIfNeeded(btcAddressGenerationConfig.nodeId)
        }
    }

    private fun initIrohaObservable(irohaObservable: Observable<BlockOuterClass.Block>) {
        irohaObservable.subscribeOn(
            Schedulers.from(
                createPrettySingleThreadPool(
                    BTC_ADDRESS_GENERATION_SERVICE_NAME,
                    "iroha-block-handler"
                )
            )
        ).subscribe({ block ->
            getSetDetailCommands(block).forEach { command ->
                if (isNewClientRegistered(command)) {
                    // generate new multisignature address if new client has been registered recently
                    addressGenerationTrigger.startFreeAddressGenerationIfNeeded(
                        btcAddressGenerationConfig.threshold,
                        btcAddressGenerationConfig.nodeId
                    ).fold(
                        { "Free BTC address generation was triggered" },
                        { ex -> logger.error("Cannot trigger address generation", ex) })
                } else if (isAddressGenerationTriggered(command)) {
                    //add new public key to session account, if trigger account was changed
                    val sessionAccountName = command.setAccountDetail.key
                    onGenerateKey(sessionAccountName).fold(
                        { pubKey -> logger.info { "New public key $pubKey for BTC multisignature address was created" } },
                        { ex -> logger.error("Cannot generate public key for BTC multisignature address", ex) })
                } else if (isNewKey(command)) {
                    val accountId = command.setAccountDetail.accountId
                    //create multisignature address, if we have enough keys in session account
                    onGenerateMultiSigAddress(accountId, getAddressTypeByAccountId(accountId)).failure { ex ->
                        logger.error(
                            "Cannot generate multi signature address", ex
                        )
                    }
                }
            }
        }, { ex ->
            notHealthy()
            logger.error("Error on subscribe", ex)
        })
    }

    // Checks if new client was registered
    private fun isNewClientRegistered(command: Commands.Command): Boolean {
        val setAccountDetail = command.setAccountDetail
        return setAccountDetail.accountId.endsWith(CLIENT_DOMAIN) && setAccountDetail.key == BTC_CURRENCY_NAME_KEY
    }

    // Checks if address generation account was triggered
    private fun isAddressGenerationTriggered(command: Commands.Command) =
        command.setAccountDetail.accountId == btcAddressGenerationConfig.pubKeyTriggerAccount

    // Checks if new key was added
    private fun isNewKey(command: Commands.Command) =
        command.setAccountDetail.accountId.endsWith("btcSession")
                && command.setAccountDetail.key != ADDRESS_GENERATION_TIME_KEY
                && command.setAccountDetail.key != ADDRESS_GENERATION_NODE_ID_KEY

    // Generates new key
    private fun onGenerateKey(sessionAccountName: String): Result<String, Exception> {
        return btcPublicKeyProvider.createKey(sessionAccountName) { saveWallet() }
    }

    /**
     * Generates multisig address
     * @param sessionAccount - account that holds public keys that are used in multisig address generation
     * @param addressType - type of address to generate
     */
    private fun onGenerateMultiSigAddress(
        sessionAccount: String,
        addressType: BtcAddressType
    ): Result<Unit, Exception> {
        return getAccountDetails(
            registrationQueryAPI,
            sessionAccount,
            btcAddressGenerationConfig.registrationAccount.accountId
        ).flatMap { details ->
            // Getting time
            val time = details.remove(ADDRESS_GENERATION_TIME_KEY)!!.toLong()
            // Getting node id
            val nodeId = details.remove(ADDRESS_GENERATION_NODE_ID_KEY)!!
            // Getting keys
            val notaryKeys = details.values
            if (!notaryKeys.isEmpty()) {
                btcPublicKeyProvider.checkAndCreateMultiSigAddress(
                    notaryKeys.toList(),
                    addressType,
                    time,
                    nodeId
                ) { saveWallet() }
            } else {
                Result.of { Unit }
            }
        }
    }

    // Safes wallet full of keys
    private fun saveWallet() {
        keysWallet.safeSave(btcAddressGenerationConfig.btcKeysWalletPath)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
