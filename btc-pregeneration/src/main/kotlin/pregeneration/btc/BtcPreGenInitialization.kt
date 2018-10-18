package pregeneration.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import healthcheck.HealthyServiceInitializer
import io.reactivex.Observable
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import model.IrohaCredential
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import pregeneration.btc.config.BtcPreGenConfig
import provider.btc.BtcPublicKeyProvider
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.getAccountDetails
import java.util.concurrent.atomic.AtomicBoolean

/*
   This class listens to special account to be triggered and starts pregeneration process
 */
@Component
class BtcPreGenInitialization(
    @Qualifier("registrationCredential")
    @Autowired private val registrationCredential: IrohaCredential,
    @Autowired private val irohaNetwork: IrohaNetwork,
    @Autowired private val btcPreGenConfig: BtcPreGenConfig,
    @Autowired private val btcPublicKeyProvider: BtcPublicKeyProvider,
    @Autowired private val irohaChainListener: IrohaChainListener
) : HealthyServiceInitializer {
    private val healthy = AtomicBoolean(true)

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
                if (command.setAccountDetail.accountId == btcPreGenConfig.pubKeyTriggerAccount) {
                    //add new public key to session account, if trigger account was changed
                    val sessionAccountName = command.setAccountDetail.key
                    onGenerateKey(sessionAccountName).fold(
                        { pubKey -> logger.info { "New public key $pubKey for BTC multisignature address was created" } },
                        { ex -> logger.error("Cannot generate public key for BTC multisignature address", ex) })
                } else if (command.setAccountDetail.accountId.endsWith("btcSession")) {
                    //create multisignature address, if we have enough keys in session account
                    onGenerateMultiSigAddress(command.setAccountDetail.accountId).failure { ex ->
                        logger.error(
                            "Cannot generate multi signature address", ex
                        )
                    }
                }
            }
        }, { ex ->
            healthy.set(false)
            logger.error("Error on subscribe", ex)
        })
    }

    private fun getSetDetailCommands(block: BlockOuterClass.Block): List<Commands.Command> {
        return block.payload.transactionsList.flatMap { tx -> tx.payload.reducedPayload.commandsList }
            .filter { command -> command.hasSetAccountDetail() }
    }

    private fun onGenerateKey(sessionAccountName: String): Result<String, Exception> {
        return btcPublicKeyProvider.createKey(sessionAccountName)
    }

    private fun onGenerateMultiSigAddress(sessionAccount: String): Result<Unit, Exception> {
        return getAccountDetails(
            registrationCredential,
            irohaNetwork,
            sessionAccount,
            btcPreGenConfig.registrationAccount.accountId
        ).flatMap { details ->
            val notaryKeys = details.values
            btcPublicKeyProvider.checkAndCreateMultiSigAddress(notaryKeys)
        }
    }

    override fun isHealthy() = healthy.get()

    /**
     * Logger
     */
    companion object : KLogging()

}
