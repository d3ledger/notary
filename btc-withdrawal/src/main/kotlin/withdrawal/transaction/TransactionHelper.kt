package withdrawal.transaction

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import mu.KLogging
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionOutput
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import provider.btc.address.BtcRegisteredAddressesProvider
import provider.btc.network.BtcNetworkConfigProvider

//TODO develop more sophisticated mechanism to compute fee
private const val MIN_FEE = 1000

/*
    Helper class that is used to collect inputs, outputs and etc
 */
@Component
class TransactionHelper(
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider,
    @Autowired private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider
) {

    private val usedOutputs = HashSet<TransactionOutput>()

    /**
     * Adds outputs(destination and change addresses) to a given transaction
     * @param transaction - current transaction
     * @param unspents - used to compute change
     * @param destinationAddress - receiver's base58 Bitcoin address
     * @param amount - amount of SAT to spend(used to compute change)
     * @param changeAddress - address that is used to store change
     */
    fun addOutputs(
        transaction: Transaction,
        unspents: List<TransactionOutput>,
        destinationAddress: String,
        amount: Long,
        changeAddress: Address
    ) {
        val totalAmount = getTotalUnspentValue(unspents)
        transaction.addOutput(
            Coin.valueOf(amount),
            Address.fromBase58(btcNetworkConfigProvider.getConfig(), destinationAddress)
        )
        val change = totalAmount - amount - MIN_FEE
        //TODO create change address creation mechanism
        transaction.addOutput(Coin.valueOf(change), changeAddress)
    }

    /**
     * Collects previously sent transactions, that may be used as an input for newly created transaction
     * @param availableAddresses - set of addresses which transactions will be available to spend
     * @param wallet - current wallet. Used to fetch unspents
     * @param amount - amount of SAT to spend
     * @param confidenceLevel - minimum depth of transactions
     * @return result with list full of unspent transactions
     */
    fun collectUnspents(
        availableAddresses: Set<String>,
        wallet: Wallet,
        amount: Long,
        confidenceLevel: Int
    ): Result<List<TransactionOutput>, Exception> {
        return Result.of {
            //Only confirmed transactions must be used
            val unspents =
                ArrayList<TransactionOutput>(wallet.unspents.filter { unspent ->
                    unspent.parentTransactionDepthInBlocks >= confidenceLevel && !usedOutputs.contains(
                        unspent
                    )
                })
            /*
            Wallet stores unspents in a HashSet, while the order of HashSet depends on several factors: current array size and etc.
            This may lead different notary nodes to pick different transactions.
            This is why we order transactions manually, essentially reducing the probability of
            different nodes to pick different transactions*/
            unspents.sortWith(Comparator { output1, output2 -> output1.hashCode().compareTo(output2.hashCode()) })
            val usedUnspents = ArrayList<TransactionOutput>()
            val totalAmount = amount + MIN_FEE
            var collectedAmount = 0L
            //We use registered clients outputs only
            unspents.filter { unspent -> isAvailableOutput(availableAddresses, unspent) }.forEach { unspent ->
                if (collectedAmount >= totalAmount) {
                    return@forEach
                }
                collectedAmount += unspent.value.value
                usedUnspents.add(unspent)
            }
            if (collectedAmount < totalAmount) {
                throw IllegalStateException("Cannot get enough BTC amount(required $amount, collected $collectedAmount) using current unspent tx collection")
            }
            unspents
        }
    }

    /**
     * Registers given transaction outputs as "untouchable" to use in the future
     * @param unspents - transaction outputs to register as "untouchable"
     */
    fun registerUnspents(unspents: List<TransactionOutput>) {
        usedOutputs.addAll(unspents)
    }

    // Computes total unspend value
    private fun getTotalUnspentValue(unspents: List<TransactionOutput>): Long {
        var totalValue = 0L
        unspents.forEach { unspent -> totalValue += unspent.value.value }
        return totalValue
    }

    // Checks if transaction output was addressed to available address
    private fun isAvailableOutput(availableAddresses: Set<String>, output: TransactionOutput): Boolean {
        return availableAddresses.contains(output.scriptPubKey.getToAddress(output.params).toBase58())
    }

    // Fetches available(registered) addresses( intersection between watched and registered addresses)
    fun getAvailableAddresses(wallet: Wallet): Result<Set<String>, Exception> {
        return btcRegisteredAddressesProvider.getRegisteredAddresses()
            .map { registeredAddresses ->
                registeredAddresses.keys.filter { btcAddress ->
                    wallet.isAddressWatched(
                        Address.fromBase58(
                            btcNetworkConfigProvider.getConfig(),
                            btcAddress
                        )
                    )
                }.toSet()
            }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
