package sideChain.eth

import main.CONFIG
import main.ConfigKeys
import mu.KLogging
import notary.NotaryInputEvent
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.Transaction
import org.web3j.protocol.http.HttpService
import sideChain.ChainHandler
import util.hexToAscii
import java.math.BigInteger

/**
 * Implementation of [ChainHandler] for Ethereum side chain.
 * Extract interesting transactions from Ethereum block.
 * @param web3 - endpoint of Ethereum client
 */
class EthChainHandler(val web3: Web3j) : ChainHandler<EthBlock> {

    /**
     * Parse [EthBlock] for transactions.
     * @return List of transation we are interested in
     */
    override fun parseBlock(block: EthBlock): List<NotaryInputEvent> {
        logger.info { "Eth chain handler for block ${block.block.number}" }

        return block.block.transactions
            .map { it.get() as Transaction }
            .filter {
                // listen to Ether wallet or ERC20 token wallet
                it.to == CONFIG[ConfigKeys.ethListenAddress].toLowerCase() ||
                        it.to == CONFIG[ConfigKeys.ethListenERC20Address].toLowerCase()

            }
            // get transaction receipt
            .flatMap {
                val receipt = web3.ethGetTransactionReceipt(it.hash).send()
                receipt.transactionReceipt.get().logs
            }
            .filter {
                // filter out transfer
                // the first topic is hashed representation of transfer signature call
                it.topics[0] == "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"
            }
            .map {
                // second and third topics are addresses from and to
                val from = "0x" + it.topics[1].drop(26).toLowerCase()
                val to = "0x" + it.topics[2].drop(26).toLowerCase()
                // amount of tranfer is stored in data
                val amount = BigInteger(it.data.drop(2), 16)

                NotaryInputEvent.EthChainInputEvent.OnEthSidechainDepositToken(
                    from,
                    "tkn",
                    amount
                )
            }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
