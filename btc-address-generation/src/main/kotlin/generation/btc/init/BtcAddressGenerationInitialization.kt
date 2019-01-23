package generation.btc.init

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import generation.btc.config.BtcAddressGenerationConfig
import generation.btc.trigger.AddressGenerationTrigger
import healthcheck.HealthyService
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import jp.co.soramitsu.iroha.java.QueryAPI
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import provider.btc.account.BTC_CURRENCY_NAME_KEY
import provider.btc.address.BtcAddressType
import provider.btc.address.getAddressTypeByAccountId
import provider.btc.generation.ADDRESS_GENERATION_TIME_KEY
import provider.btc.generation.BtcPublicKeyProvider
import sidechain.iroha.CLIENT_DOMAIN
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.util.getAccountDetails
import sidechain.iroha.util.getSetDetailCommands
import java.util.concurrent.Executors

/*
   This class listens to special account to be triggered and starts generation process
 */
@Component
class BtcAddressGenerationInitialization(
    @Qualifier("registrationQueryAPI")
    @Autowired private val registrationQueryAPI: QueryAPI,
    @Autowired private val btcAddressGenerationConfig: BtcAddressGenerationConfig,
    @Autowired private val btcPublicKeyProvider: BtcPublicKeyProvider,
    @Autowired private val irohaChainListener: IrohaChainListener,
    @Autowired private val addressGenerationTrigger: AddressGenerationTrigger
) : HealthyService() {

    /*
    Initiates listener that listens to events in trigger account.
    If trigger account is triggered, new session account full notary public keys will be created
     */
    fun init(): Result<Unit, Exception> {
        return irohaChainListener.getBlockObservable()
            .map { irohaObservable ->
                initIrohaObservable(irohaObservable)
            }.flatMap {
                // Start address generation at initial phase
                addressGenerationTrigger
                    .startFreeAddressGenerationIfNeeded(btcAddressGenerationConfig.threshold)
            }
    }

    private fun initIrohaObservable(irohaObservable: Observable<BlockOuterClass.Block>) {
        irohaObservable.subscribeOn(Schedulers.from(Executors.newSingleThreadExecutor())).subscribe({ block ->
            getSetDetailCommands(block).forEach { command ->
                if (isNewClientRegistered(command)) {
                    // generate new multisignature address if new client has been registered recently
                    addressGenerationTrigger.startFreeAddressGenerationIfNeeded(btcAddressGenerationConfig.threshold)
                        .fold(
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
    private fun isNewKey(command: Commands.Command) = command.setAccountDetail.accountId.endsWith("btcSession")

    // Generates new key
    private fun onGenerateKey(sessionAccountName: String): Result<String, Exception> {
        return btcPublicKeyProvider.createKey(sessionAccountName)
    }

    // Generates multisig address
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
            // Getting keys
            val notaryKeys = details.values
            if (!notaryKeys.isEmpty()) {
                btcPublicKeyProvider.checkAndCreateMultiSigAddress(notaryKeys, addressType, time)
            } else {
                Result.of { Unit }
            }
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
