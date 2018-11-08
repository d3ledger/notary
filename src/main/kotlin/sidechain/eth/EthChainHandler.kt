package sidechain.eth

import com.github.kittinunf.result.fanout
import mu.KLogging
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.Transaction
import org.web3j.protocol.parity.Parity
import org.web3j.protocol.parity.methods.response.Trace
import provider.eth.ETH_PRECISION
import provider.eth.EthRelayProvider
import provider.eth.EthTokensProvider
import sidechain.ChainHandler
import sidechain.SideChainEvent
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Implementation of [ChainHandler] for Ethereum side chain.
 * Extract interesting transactions from Ethereum block.
 * @param web3 - notary.endpoint of Ethereum client
 * @param ethRelayProvider - provider of observable wallets
 * @param ethTokensProvider - provider of observable tokens
 */
class EthChainHandler(
    val web3: Parity,
    val ethRelayProvider: EthRelayProvider,
    val ethTokensProvider: EthTokensProvider
) :
    ChainHandler<EthBlock> {

    /**
     * Process Ethereum ERC20 tokens
     * @param tx transaction in block
     * @return list of notary events on ERC20 deposit
     */
    private fun handleErc20(
        tx: Transaction,
        time: BigInteger,
        wallets: Map<String, String>,
        tokens: Map<String, String>
    ): List<SideChainEvent.PrimaryBlockChainEvent> {
        logger.info { "Handle ERC20 tx ${tx.hash}" }

        // get receipt that contains data about solidity function execution
        val receipt = web3.ethGetTransactionReceipt(tx.hash).send()

        // if tx is committed successfully
        if (receipt.transactionReceipt.get().isStatusOK) {
            return receipt.transactionReceipt.get().logs
                .filter {
                    // filter out transfer
                    // the first topic is a hashed representation of a transfer signature call (the scary string)
                    val to = "0x" + it.topics[2].drop(26).toLowerCase()
                    it.topics[0] == "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef" &&
                            wallets.containsKey(to)
                }
                .filter {
                    // check if amount > 0
                    if (BigInteger(it.data.drop(2), 16).compareTo(BigInteger.ZERO) > 0) {
                        true
                    } else {
                        logger.warn { "Transaction ${tx.hash} from Ethereum with 0 ERC20 amount" }
                        false
                    }
                }
                .map {
                    // all non-existent keys were filtered out in parseBlock
                    val tokenName = tokens[tx.to]!!
                    ethTokensProvider.getTokenPrecision(tokenName)
                        .fold(
                            { precision ->
                                // second and third topics are addresses from and to
                                val from = "0x" + it.topics[1].drop(26).toLowerCase()
                                val to = "0x" + it.topics[2].drop(26).toLowerCase()
                                // amount of transfer is stored in data
                                val amount = BigInteger(it.data.drop(2), 16)

                                SideChainEvent.PrimaryBlockChainEvent.OnPrimaryChainDeposit(
                                    tx.hash,
                                    time,
                                    wallets[to]!!,
                                    tokenName,
                                    BigDecimal(amount, precision.toInt()).toPlainString(),
                                    from
                                )
                            },
                            { throw it }
                        )
                }
        } else {
            return listOf()
        }
    }

    /**
     * Process Ether deposit
     * @param tx transaction in block
     * @return list of notary events on Ether deposit
     */
    private fun handleEther(
        hash: String,
        from: String,
        to: String,
        value: BigInteger,
        time: BigInteger,
        wallets: Map<String, String>
    ): List<SideChainEvent.PrimaryBlockChainEvent> {
        logger.info { "Handle Ethereum tx $hash" }

        val receipt = web3.ethGetTransactionReceipt(hash).send()

        return if (!receipt.transactionReceipt.get().isStatusOK) {
            logger.warn { "Transaction $hash from Ethereum has FAIL status" }
            listOf()
        } else if (value <= BigInteger.ZERO) {
            logger.warn { "Transaction $hash from Ethereum with 0 ETH amount" }
            listOf()
        } else {
            // if tx amount > 0 and is committed successfully
            listOf(
                SideChainEvent.PrimaryBlockChainEvent.OnPrimaryChainDeposit(
                    hash,
                    time,
                    // all non-existent keys were filtered out in parseBlock
                    wallets[to]!!,
                    "ether",
                    BigDecimal(value, ETH_PRECISION.toInt()).toPlainString(),
                    from
                )
            )
        }
    }

    /**
     * Parse [EthBlock] for transactions.
     * @return List of transactions we are interested in
     */
    override fun parseBlock(block: EthBlock): List<SideChainEvent.PrimaryBlockChainEvent> {
        logger.info { "Eth chain handler for block ${block.block.number}" }

        return ethRelayProvider.getRelays().fanout {
            ethTokensProvider.getTokens()
        }.fold(
            { (wallets, tokens) ->
                // Eth time in seconds, convert ot milliseconds
                val time = block.block.timestamp.multiply(BigInteger.valueOf(1000))
                block.block.transactions
                    .map { it.get() as Transaction }
                    .flatMap { tx ->
                        val receipt = web3.traceTransaction(tx.hash).send()
                        receipt?.result?.forEach {
                            val ac = it.action
                            if (ac is Trace.CallAction && wallets.containsKey(ac.to)) {
                                return handleEther(tx.hash, tx.from, ac.to, ac.value, time, wallets)
                            }
                        }

                        if (tokens.containsKey(tx.to))
                            handleErc20(tx, time, wallets, tokens)
                        else
                            listOf()
                    }
            },
            { ex ->
                logger.error("Cannot parse block", ex)
                listOf()
            }
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
