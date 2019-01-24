package generation.btc.trigger

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import provider.TriggerProvider
import provider.btc.address.BtcAddressType
import provider.btc.generation.BtcSessionProvider

/*
    Class that is used to start address generation process
 */
@Component
class AddressGenerationTrigger(
    @Autowired private val btcSessionProvider: BtcSessionProvider,
    @Autowired private val btcAddressGenerationTriggerProvider: TriggerProvider
) {
    /**
     * Starts address generation process
     * @param addressType - type of address to generate
     */
    fun startAddressGeneration(addressType: BtcAddressType): Result<Unit, Exception> {
        val sessionAccountName = addressType.createSessionAccountName()
        return btcSessionProvider.createPubKeyCreationSession(sessionAccountName)
            .flatMap {
                btcAddressGenerationTriggerProvider.trigger(
                    sessionAccountName
                )
            }
    }
}
