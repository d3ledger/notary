package sideChain.eth

import hu.akarnokd.rxjava.interop.RxJavaInterop
import io.reactivex.Observable
import mu.KLogging
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.http.HttpService
import sideChain.ChainListener
import java.math.BigInteger

/**
 * Implementation of [ChainListener] for Ethereum sidechain
 */
class EthChainListener : ChainListener<EthBlock> {

    /** Confirmation period is the number of blocks that we assume that may be reorganised */
    private val confirmationPeriod = BigInteger.valueOf(6)

    /** Keep counting blocks to prevent double emitting in case of chain reorganisation */
    private var lastBlock = confirmationPeriod

    /**
     * Emit event when new block in Ethereum is committed
     */
    override fun onNewBlockObservable(): Observable<EthBlock> {
        // subscribe to a client
        val web3 = Web3j.build(HttpService("http://0.0.0.0:8180/#/auth?token=P8VD-nlD5-8AB5-ZIJv"))

        // convert rx1 to rx2
        return RxJavaInterop.toV2Observable(web3.blockObservable(false))
            // skip up to confirmationPeriod blocks in case of chain reorganisation
            .filter { lastBlock < it.block.number }
            .map {
                lastBlock = it.block.number
                val block = web3.ethGetBlockByNumber(
                    DefaultBlockParameter.valueOf(
                        it.block.number - confirmationPeriod
                    ), false
                ).send()
                block
            }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
