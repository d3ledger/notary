package withdrawalservice

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import io.reactivex.Observable
import mu.KLogging
import sidechain.SideChainEvent
import sidechain.eth.consumer.EthConsumer
import sidechain.iroha.IrohaChainListener

class WithdrawalServiceInitialization {

    /**
     * Init Iroha chain listener
     * @return Observable on Iroha sidechain events
     */
    private fun initIrohaChain(): Result<Observable<SideChainEvent.IrohaEvent>, Exception> {
        logger.info { "Init Iroha chain listener" }

        return IrohaChainListener().getBlockObservable()
            .map { observable ->
                observable
                    .flatMapIterable { block ->
                        logger.info { "Convert iroha block to withdrawal input event" }
                        block.payload.transactionsList
                            .flatMap { it.payload.commandsList }
                            .filter { it.hasTransferAsset() }
                            .map { SideChainEvent.IrohaEvent.OnIrohaSideChainTransfer.fromProto(it.toByteArray()) }
                            .map { SideChainEvent.IrohaEvent.OnIrohaSideChainTransfer(it) as SideChainEvent.IrohaEvent }
                    }
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
