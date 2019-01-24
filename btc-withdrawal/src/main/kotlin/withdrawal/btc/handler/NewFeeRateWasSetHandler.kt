package withdrawal.btc.handler

import fee.FeeRate
import iroha.protocol.Commands
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

private const val MIN_FEE_RATE = 10

/**
 * Handler that handles 'set new fee rate' events
 */
@Component
class NewFeeRateWasSetHandler(
    @Qualifier("btcFeeRateAccount")
    @Autowired private val btcFeeRateAccount: String
) {

    fun handleNewFeeRate(command: Commands.Command) {
        if (isNewFeeRateWasSet(command)) {
            val feeRate = FeeRate.fromJson(command.setAccountDetail.value)!!
            CurrentFeeRate.set(feeRate.avgFeeRate)
        }
    }

    private fun isNewFeeRateWasSet(command: Commands.Command) = command.setAccountDetail.accountId == btcFeeRateAccount
}

/**
 * Singleton object that holds information abount current fee rate
 */
object CurrentFeeRate {
    private const val NOT_SET_FEE_RATE = -1

    private var feeRate = AtomicInteger(NOT_SET_FEE_RATE)
    /**
     * Checks if fee rate was set
     */
    fun isPresent() = feeRate.get() > NOT_SET_FEE_RATE

    /**
     * Returns current fee rate
     */
    fun get() = Math.max(feeRate.get(), MIN_FEE_RATE)

    /**
     * Sets fee rate
     * @param feeRate - fee rate
     */
    fun set(feeRate: Int) = this.feeRate.set(feeRate)

    /**
     * Clears current fee rate.
     * After this call fee rate will be considered as 'no set'
     */
    fun clear() = set(NOT_SET_FEE_RATE)

}
