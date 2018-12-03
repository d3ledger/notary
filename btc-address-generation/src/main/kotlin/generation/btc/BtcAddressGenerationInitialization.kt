package generation.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import generation.btc.config.BtcAddressGenerationConfig
import healthcheck.HealthyService
import io.reactivex.Observable
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import model.IrohaCredential
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import provider.btc.BtcPublicKeyProvider
import provider.btc.address.BtcAddressType
import provider.btc.address.getAddressTypeByAccountId
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.getAccountDetails
import sidechain.iroha.util.getSetDetailCommands

/*
   This class listens to special account to be triggered and starts generation process
 */
@Component
class BtcAddressGenerationInitialization(
    @Qualifier("registrationCredential")
    @Autowired private val registrationCredential: IrohaCredential,
    @Autowired private val irohaNetwork: IrohaNetwork,
    @Autowired private val btcAddressGenerationConfig: BtcAddressGenerationConfig,
    @Autowired private val btcPublicKeyProvider: BtcPublicKeyProvider,
    @Autowired private val irohaChainListener: IrohaChainListener
) : HealthyService() {

    /*
    Initiates listener that listens to events in trigger account.
    If trigger account is triggered, new session account full notary public keys will be created
     */
    fun init(): Result<Unit, Exception> {
        return irohaChainListener.getBlockObservable().map { irohaObservable ->
            initIrohaObservable(irohaObservable)
        }
    }

    private fun initIrohaObservable(irohaObservable: Observable<BlockOuterClass.Block>) {
        irohaObservable.subscribe({ block ->
            getSetDetailCommands(block).forEach { command ->
                if (isAddressGenerationTriggered(command)) {
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

    // Checks if address generation account was triggered
    private fun isAddressGenerationTriggered(command: Commands.Command) =
        command.setAccountDetail.accountId == btcAddressGenerationConfig.pubKeyTriggerAccount

    // Check if new key was added
    private fun isNewKey(command: Commands.Command) = command.setAccountDetail.accountId.endsWith("btcSession")

    private fun onGenerateKey(sessionAccountName: String): Result<String, Exception> {
        return btcPublicKeyProvider.createKey(sessionAccountName)
    }

    private fun onGenerateMultiSigAddress(
        sessionAccount: String,
        addressType: BtcAddressType
    ): Result<Unit, Exception> {
        return getAccountDetails(
            registrationCredential,
            irohaNetwork,
            sessionAccount,
            btcAddressGenerationConfig.registrationAccount.accountId
        ).flatMap { details ->
            val notaryKeys = details.values
            btcPublicKeyProvider.checkAndCreateMultiSigAddress(notaryKeys, addressType)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
