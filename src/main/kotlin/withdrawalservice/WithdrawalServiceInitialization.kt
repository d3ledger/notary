package withdrawalservice

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.ConfigKeys
import io.reactivex.Observable
import mu.KLogging
import notary.CONFIG
import sidechain.SideChainEvent
import sidechain.eth.consumer.EthConsumer
import sidechain.iroha.IrohaChainHandler
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.util.ModelUtil

class WithdrawalServiceInitialization {
    val irohaAccount = CONFIG[ConfigKeys.notaryIrohaAccount]
    val irohaKeypair =
        ModelUtil.loadKeypair(CONFIG[ConfigKeys.notaryPubkeyPath], CONFIG[ConfigKeys.notaryPrivkeyPath]).get()


    /**
     * Init Iroha chain listener
     * @return Observable on Iroha sidechain events
     */
    private fun initIrohaChain(): Result<Observable<SideChainEvent.IrohaEvent>, Exception> {
        logger.info { "Init Iroha chain" }

        return IrohaChainListener(irohaAccount, irohaKeypair).getBlockObservable()
            .map { observable ->
                observable.flatMapIterable { IrohaChainHandler().parseBlock(it) }
            }
    }

    /**
     * Init Withdrawal Service
     */
    private fun initWithdrawalService(inputEvents: Observable<SideChainEvent.IrohaEvent>): WithdrawalService {
        logger.info { "Init Withdrawal Service" }

        return WithdrawalServiceImpl(inputEvents)
    }

    private fun initEthConsumer(withdrawalService: WithdrawalService): Result<Unit, Exception> {
        logger.info { "Init Ether consumer" }

        return Result.of {
            val ethConsumer = EthConsumer()
            withdrawalService.output()
                .subscribe({
                    ethConsumer.consume(it)
                }, {
                    logger.error { it }
                })
            Unit
        }
    }

    fun init(): Result<Unit, Exception> {
        logger.info { "Withdrawal Service initialization" }

        return initIrohaChain()
            .map { initWithdrawalService(it) }
            .flatMap { initEthConsumer(it) }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
