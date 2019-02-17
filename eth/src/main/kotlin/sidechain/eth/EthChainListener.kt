package sidechain.eth

import com.github.kittinunf.result.Result
import io.reactivex.Observable
import mu.KLogging
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.parity.Parity
import sidechain.ChainListener
import java.math.BigInteger

/**
 * Implementation of [ChainListener] for Ethereum sidechain
 * @param web3 - notary.endpoint of Ethereum client
 * @param confirmationPeriod - number of block to consider block final
 */
class EthChainListener(
    private val web3: Parity,
    private val confirmationPeriod: BigInteger
) : ChainListener<EthBlock> {

    init {
        logger.info {
            "Init EthChainListener with confirmation period $confirmationPeriod"
        }
    }

    /** Keep counting blocks to prevent double emitting in case of chain reorganisation */
    private var lastBlock = confirmationPeriod

    override fun getBlockObservable(autoAck : Boolean): Result<Observable<EthBlock>, Exception> {
        return Result.of {
            web3.blockFlowable(false).toObservable()
                // skip up to confirmationPeriod blocks in case of chain reorganisation
                .filter { lastBlock < it.block.number }
                .map {
                    lastBlock = it.block.number
                    val block = web3.ethGetBlockByNumber(
                        DefaultBlockParameter.valueOf(
                            it.block.number - confirmationPeriod
                        ), true
                    ).send()
                    block
                }
        }
    }

    /**
     * @return a block as soon as it is committed to Ethereum
     */
    override suspend fun getBlock(autoAck : Boolean): EthBlock {
        return getBlockObservable().get().blockingFirst()
    }

    override fun close() {
        web3.shutdown()
    }
    
    /**
     * Logger
     */
    companion object : KLogging()
}
