package sideChain.eth

import com.github.kittinunf.result.Result
import hu.akarnokd.rxjava.interop.RxJavaInterop
import io.reactivex.Observable
import main.Configs
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
    private val confirmationPeriod = BigInteger.valueOf(Configs.ethConfirmationPeriod)

    /** Keep counting blocks to prevent double emitting in case of chain reorganisation */
    private var lastBlock = confirmationPeriod

    override fun getBlockObservable(): Result<Observable<EthBlock>, Exception> {
        return Result.of {
            // subscribe to a client
            val web3 = Web3j.build(HttpService(Configs.ethConnectionUrl))

            // convert rx1 to rx2
            RxJavaInterop.toV2Observable(web3.blockObservable(false))
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
     * Logger
     */
    companion object : KLogging()
}
