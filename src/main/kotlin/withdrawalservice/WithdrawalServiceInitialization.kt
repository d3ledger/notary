package withdrawalservice

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import io.reactivex.Observable
import mu.KLogging
import sideChain.eth.consumer.EthConsumer
import sideChain.iroha.IrohaChainListener

class WithdrawalServiceInitialization {

    private var irohaChainListener = IrohaChainListener()

    private lateinit var withdrawalService: WithdrawalService
    private lateinit var ethConsumer: EthConsumer


    /**
     * Init Iroha chain listener
     * @return Observable on Iroha sidechain events
     */
    private fun initIrohaChain(): Result<Observable<WithdrawalServiceInputEvent>, Exception> {
        logger.info { "Init Iroha chain" }

        return irohaChainListener.getBlockObservable()
            .map { observable ->
                // TODO a.chernyshov: rework with effective realization
                observable.map {
                    logger.info { "Convert iroha block to withdrawal input event" }

                    WithdrawalServiceInputEvent.IrohaInputEvent.Withdrawal() as WithdrawalServiceInputEvent
                }
            }
    }

    /**
     * Init Withdrawal Service
     */
    private fun initWithdrawalService(inputEvents: Observable<WithdrawalServiceInputEvent>) {
        logger.info { "Init Withdrawal Service" }

        withdrawalService = WithdrawalServiceImpl(inputEvents)
    }

    private fun initEthConsumer(): Result<Unit, Exception> {
        logger.info { "Init Ether consumer" }

        return Result.of {
            ethConsumer = EthConsumer()
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
            .flatMap { initEthConsumer() }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
