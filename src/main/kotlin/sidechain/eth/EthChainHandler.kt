package sidechain.eth

import com.github.kittinunf.result.fanout
import mu.KLogging
import provider.EthTokensProvider
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.Transaction
import provider.EthRelayProvider
import sidechain.ChainHandler
import sidechain.SideChainEvent
import java.math.BigInteger

/**
 * Implementation of [ChainHandler] for Ethereum side chain.
 * Extract interesting transactions from Ethereum block.
 * @param web3 - notary.endpoint of Ethereum client
 * @param ethRelayProvider - provider of observable wallets
 * @param ethTokensProvider - provider of observable tokens
 */
class EthChainHandler(
    val web3: Web3j,
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
        wallets: Map<String, String>,
        tokens: Map<String, String>
    ): List<SideChainEvent> {
        logger.info { "handle ERC20 tx ${tx.hash}" }

        // get receipt that contains data about solidity function execution
        val receipt = web3.ethGetTransactionReceipt(tx.hash).send()

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
                // second and third topics are addresses from and to
                val from = "0x" + it.topics[1].drop(26).toLowerCase()
                val to = "0x" + it.topics[2].drop(26).toLowerCase()
                // amount of transfer is stored in data
                val amount = BigInteger(it.data.drop(2), 16)

                SideChainEvent.EthereumEvent.OnEthSidechainDepositToken(
                    tx.hash,
                    wallets[to]!!,
                    // all non-existent keys were filtered out in parseBlock
                    tokens[tx.to]!!,
                    amount,
                    from
                )
            }
    }

    /**
     * Process Ether deposit
     * @param tx transaction in block
     * @return list of notary events on Ether deposit
     */
    private fun handleEther(tx: Transaction, wallets: Map<String, String>): List<SideChainEvent> {
        logger.info { "handle Ethereum tx ${tx.hash}" }

        return if (tx.value.compareTo(BigInteger.ZERO) > 0) {
            listOf(
                SideChainEvent.EthereumEvent.OnEthSidechainDeposit(
                    tx.hash,
                    // all non-existent keys were filtered out in parseBlock
                    wallets[tx.to]!!,
                    tx.value,
                    tx.from
                )
            )
        } else {
            logger.warn { "Transaction ${tx.hash} from Ethereum with 0 ETH amount" }
            listOf()
        }
    }

    /**
     * Parse [EthBlock] for transactions.
     * @return List of transation we are interested in
     */
    override fun parseBlock(block: EthBlock): List<SideChainEvent> {
        logger.info { "Eth chain handler for block ${block.block.number}" }

        return ethRelayProvider.getRelays().fanout {
            ethTokensProvider.getTokens()
        }.fold(
            { (wallets, tokens) ->
                block.block.transactions
                    .map { it.get() as Transaction }
                    .flatMap {
                        if (wallets.containsKey(it.to))
                            handleEther(it, wallets)
                        else if (tokens.containsKey(it.to))
                            handleErc20(it, wallets, tokens)
                        else
                            listOf()
                    }
            }, {
                logger.error { it }
                listOf()
            }
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
