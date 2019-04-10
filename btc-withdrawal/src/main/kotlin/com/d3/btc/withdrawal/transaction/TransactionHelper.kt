package com.d3.btc.withdrawal.transaction

import com.d3.btc.fee.BYTES_PER_INPUT
import com.d3.btc.fee.CurrentFeeRate
import com.d3.btc.fee.getTxFee
import com.d3.btc.helper.address.outPutToBase58Address
import com.d3.btc.peer.SharedPeerGroup
import com.d3.btc.provider.BtcRegisteredAddressesProvider
import com.d3.btc.provider.network.BtcNetworkConfigProvider
import com.d3.btc.provider.BtcChangeAddressProvider
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import mu.KLogging
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionOutput
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

//Only two outputs are used: destination and change
private const val OUTPUTS = 2

/*
    Helper class that is used to collect inputs, outputs and etc
 */
@Component
class TransactionHelper(
    @Autowired private val transfersWallet: Wallet,
    @Autowired private val peerGroup: SharedPeerGroup,
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
     * @param amount - amount of SAT to spend
     * @param availableHeight - maximum available height for UTXO
     * @param confidenceLevel - minimum depth of transactions
     * @return result with list full of unspent transactions
     */
    fun collectUnspents(
        availableAddresses: Set<String>,
        amount: Long,
        availableHeight: Int,
        confidenceLevel: Int
    ): Result<List<TransactionOutput>, Exception> {
        return Result.of {
            collectUnspentsRec(availableAddresses, amount, 0, availableHeight, confidenceLevel, ArrayList())
        }
    }

    /**
     * Registers given transaction outputs as "untouchable" to use in the future
     * @param tx - transaction to register
     * @param unspents - transaction outputs to register as "untouchable"
     */
    @Synchronized
    fun registerUnspents(tx: Transaction, unspents: List<TransactionOutput>) {
        usedOutputs[tx.hashAsString] = unspents
    }

    /**
     * Frees outputs, making them usable for other transactions
     * @param txHash - hash of transaction which outputs/unspents must be freed
     */
    @Synchronized
    fun unregisterUnspents(txHash: String) {
        usedOutputs.remove(txHash)
    }

    /**
     * Returns available addresses (intersection between watched and registered addresses)
     * @param generatedBefore - only addresses that were generated before certain time are considered available
     * @return result with set full of available addresses
     */
    fun getAvailableAddresses(generatedBefore: Long): Result<Set<String>, Exception> {
        return btcRegisteredAddressesProvider.getRegisteredAddresses()
            .map { registeredAddresses ->
                logger.info("Registered addresses ${registeredAddresses.map { address -> address.address }}")
                registeredAddresses.filter { btcAddress ->
                    transfersWallet.isAddressWatched(
                        Address.fromBase58(
                            btcNetworkConfigProvider.getConfig(),
                            btcAddress.address
                        )
                    )
                }.map { btcAddress -> btcAddress.address }.toMutableSet()
            }
            .fanout { btcChangeAddressProvider.getAllChangeAddresses(generatedBefore) }
            .map { (availableAddresses, changeAddresses) ->
                changeAddresses.forEach { changeAddress ->
                    //Change address is also available to use
                    availableAddresses.add(changeAddress.address)
                }
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
     * Returns currently available UTXO height
     * @param withdrawalTime - time of withdrawal
     * @param confidenceLevel - minimum depth of transactions
     */
    fun getAvailableUTXOHeight(
        confidenceLevel: Int,
        withdrawalTime: Long
    ): Result<Int, Exception> {
        return getAvailableAddresses(withdrawalTime).map { availableAddresses ->
            getAvailableUnspents(
                transfersWallet.unspents,
                Integer.MAX_VALUE,
                confidenceLevel,
                availableAddresses
            ).map { unspent -> getUnspentHeight(unspent) }.max() ?: 0
        }
    }

    /**
     * Collects previously sent transactions, that may be used as an input for newly created transaction.
     * It may go into recursion if not enough money for fee was collected.
     * @param availableAddresses - set of addresses which transactions will be available to spend
     * @param amount - amount of SAT to spend
     * @param fee - tx fee that depends on inputs and outputs. Initial value is zero.
     * @param availableHeight - maximum available height for UTXO
     * @param confidenceLevel - minimum depth of transactions
     * @param recursivelyCollectedUnspents - list of unspents collected from all recursion levels. It will be returned at the end on execution
     * @return list full of unspent transactions
     */
    private tailrec fun collectUnspentsRec(
        availableAddresses: Set<String>,
        amount: Long,
        fee: Int,
        availableHeight: Int,
        confidenceLevel: Int,
        recursivelyCollectedUnspents: MutableList<TransactionOutput>
    ): List<TransactionOutput> {
        val unspents = ArrayList(getAvailableUnspents(
            transfersWallet.unspents,
            availableHeight,
            confidenceLevel,
            availableAddresses
        ).filter { unspent -> !recursivelyCollectedUnspents.contains(unspent) })

        if (unspents.isEmpty()) {
            throw IllegalStateException("Out of unspents")
        }
        logger.info("Got unspents\n${unspents.map { unspent ->
            Pair(
                outPutToBase58Address(unspent),
                unspent.value
            )
        }}")

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
        unspents.forEach { unspent ->
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
                amount,
                newFee,
                availableHeight,
                confidenceLevel,
                recursivelyCollectedUnspents
            )
        }
        return recursivelyCollectedUnspents
    }

    /**
     * Returns currently available unspents
     * @param unspents - all the unspents that we posses
     * @param availableHeight - maximum available height for UTXO
     * @param confidenceLevel - minimum depth of transactions
     * @param availableAddresses - available addresses
     */
    @Synchronized
    private fun getAvailableUnspents(
        unspents: List<TransactionOutput>,
        availableHeight: Int,
        confidenceLevel: Int,
        availableAddresses: Set<String>
    ): List<TransactionOutput> {
        return unspents.filter { unspent ->
            // It's senseless to use 'dusty' transaction, because its fee will be higher than its value
            !isDust(unspent.value.value) &&
                    //Only confirmed unspents may be used
                    unspent.parentTransactionDepthInBlocks >= confidenceLevel
                    //Cannot use already used unspents
                    && !usedOutputs.values.flatten().contains(unspent)
                    //We are able to use those UTXOs which height is not bigger then availableHeight
                    && getUnspentHeight(unspent) <= availableHeight
                    //We use registered clients outputs only
                    && isAvailableOutput(availableAddresses, unspent)
        }
    }

    /**
     * Returns block height of a given unspent
     * @param unspent - UTXO
     * @return time of unspent transaction block
     */
    private fun getUnspentHeight(unspent: TransactionOutput): Int {
        return peerGroup.getBlock(unspent.parentTransaction!!.appearsInHashes!!.keys.first())!!.height
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
