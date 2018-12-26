package withdrawal.btc.transaction

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import fee.BYTES_PER_INPUT
import fee.getTxFee
import helper.address.outPutToBase58Address
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
import withdrawal.btc.handler.CurrentFeeRate
import withdrawal.btc.provider.BtcChangeAddressProvider

//Only two outputs are used: destination and change
private const val OUTPUTS = 2


/*
    Helper class that is used to collect inputs, outputs and etc
 */
@Component
class TransactionHelper(
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider,
    @Autowired private val btcRegisteredAddressesProvider: BtcRegisteredAddressesProvider,
    @Autowired private val btcChangeAddressProvider: BtcChangeAddressProvider
) {

    /**
     *  Map full of used transaction outputs. Key is tx hash, value is list of unspents.
     *  We need it because Bitcoinj can't say if UTXO was spent until it was not broadcasted
     *  */
    private val usedOutputs = HashMap<String, List<TransactionOutput>>()

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
        val change = totalAmount - amount - getTxFee(transaction.inputs.size, OUTPUTS, CurrentFeeRate.get())
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
            collectUnspentsRec(availableAddresses, wallet, amount, 0, confidenceLevel, ArrayList())
        }
    }

    /**
     * Registers given transaction outputs as "untouchable" to use in the future
     * @param tx - transaction to register
     * @param unspents - transaction outputs to register as "untouchable"
     */
    fun registerUnspents(tx: Transaction, unspents: List<TransactionOutput>) {
        usedOutputs[tx.hashAsString] = unspents
    }

    /**
     * Frees outputs, making them usable for other transactions
     * @param txHash - hash of transaction which outputs/unspents must be freed
     */
    fun unregisterUnspents(txHash: String) {
        usedOutputs.remove(txHash)
    }

    /**
     * Returns available addresses (intersection between watched and registered addresses)
     * @param wallet - current wallet. Used to get "watched" addresses
     * @return result with set full of available addresses
     */
    fun getAvailableAddresses(wallet: Wallet): Result<Set<String>, Exception> {
        return btcRegisteredAddressesProvider.getRegisteredAddresses()
            .map { registeredAddresses ->
                registeredAddresses.filter { btcAddress ->
                    wallet.isAddressWatched(
                        Address.fromBase58(
                            btcNetworkConfigProvider.getConfig(),
                            btcAddress.address
                        )
                    )
                }.map { btcAddress -> btcAddress.address }.toMutableSet()
            }
            .fanout { btcChangeAddressProvider.getChangeAddress() }
            .map { (availableAddresses, changeAddress) ->
                //Change address is also available to use
                availableAddresses.add(changeAddress.address)
                availableAddresses
            }
    }

    /**
     * Checks if satValue is too low to spend
     * @param satValue - amount of SAT to check if it's a dust
     * @return true, if [satValue] is a dust
     */
    fun isDust(satValue: Long) = satValue < (CurrentFeeRate.get() * BYTES_PER_INPUT)

    /**
     * Collects previously sent transactions, that may be used as an input for newly created transaction.
     * It may go into recursion if not enough money for fee was collected.
     * @param availableAddresses - set of addresses which transactions will be available to spend
     * @param wallet - current wallet. Used to fetch unspents
     * @param amount - amount of SAT to spend
     * @param fee - tx fee that depends on inputs and outputs. Initial value is zero.
     * @param confidenceLevel - minimum depth of transactions
     * @param recursivelyCollectedUnspents - list of unspents collected from all recursion levels. It will be returned at the end on execution
     * @return list full of unspent transactions
     */
    private tailrec fun collectUnspentsRec(
        availableAddresses: Set<String>,
        wallet: Wallet,
        amount: Long,
        fee: Int,
        confidenceLevel: Int,
        recursivelyCollectedUnspents: MutableList<TransactionOutput>
    ): List<TransactionOutput> {
        //Only confirmed transactions must be used
        val unspents =
            ArrayList<TransactionOutput>(wallet.unspents.filter { unspent ->
                // It's senseless to use 'dusty' transaction, because its fee will be higher than its value
                !isDust(unspent.value.value) &&
                        //Only confirmed unspents may be used
                        unspent.parentTransactionDepthInBlocks >= confidenceLevel
                        //Cannot use already used unspents
                        && !usedOutputs.values.flatten().contains(unspent)
                        //Cannot use unspents from another level of recursion
                        && !recursivelyCollectedUnspents.contains(unspent)
            })
        if (unspents.isEmpty()) {
            throw IllegalStateException("Out of unspents")
        }
        /*
        Wallet stores unspents in a HashSet. Order of a HashSet depends on several factors: current array size and etc.
        This may lead different notary nodes to pick different transactions.
        This is why we order transactions manually, essentially reducing the probability of
        different nodes to pick different output transactions.*/
        unspents.sortWith(Comparator { output1, output2 ->
            /*
            Outputs are compared by values.
            It will help us having a little amount of inputs.
            Less inputs -> smaller tx size -> smaller fee*/
            val valueComparison = -output1.value.value.compareTo(output2.value.value)
            //If values are the same, we compare by hash codes
            if (valueComparison == 0) {
                output1.hashCode().compareTo(output2.hashCode())
            } else {
                valueComparison
            }
        })
        val amountAndFee = amount + fee
        var collectedAmount = getTotalUnspentValue(recursivelyCollectedUnspents)
        //We use registered clients outputs only
        unspents.filter { unspent -> isAvailableOutput(availableAddresses, unspent) }.forEach { unspent ->
            if (collectedAmount >= amountAndFee) {
                return@forEach
            }
            collectedAmount += unspent.value.value
            recursivelyCollectedUnspents.add(unspent)
        }
        if (collectedAmount < amountAndFee) {
            throw IllegalStateException("Cannot get enough BTC amount(required $amountAndFee, collected $collectedAmount) using current unspent tx collection")
        }
        // Check if able to pay fee
        val newFee = getTxFee(recursivelyCollectedUnspents.size, OUTPUTS, CurrentFeeRate.get())
        if (collectedAmount < amount + newFee) {
            logger.info { "Not enough BTC amount(required $amount, fee $newFee, collected $collectedAmount) was collected for fee" }
            // Try to collect more unspents if no money is left for fee
            return collectUnspentsRec(
                availableAddresses,
                wallet,
                amount,
                newFee,
                confidenceLevel,
                recursivelyCollectedUnspents
            )
        }
        return recursivelyCollectedUnspents
    }

    // Computes total unspent value
    protected fun getTotalUnspentValue(unspents: List<TransactionOutput>): Long {
        var totalValue = 0L
        unspents.forEach { unspent -> totalValue += unspent.value.value }
        return totalValue
    }


    // Checks if fee output was addressed to available address
    protected fun isAvailableOutput(availableAddresses: Set<String>, output: TransactionOutput): Boolean {
        val btcAddress = outPutToBase58Address(output)
        return availableAddresses.contains(btcAddress)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
