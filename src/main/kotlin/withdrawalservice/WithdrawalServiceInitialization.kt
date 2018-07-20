package withdrawalservice

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import io.reactivex.Observable
import mu.KLogging
import sidechain.SideChainEvent
import sidechain.eth.consumer.EthConsumer
import sidechain.iroha.IrohaChainHandler
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil

/**
 * @param withdrawalConfig - configuration for withdrawal service
 */
class WithdrawalServiceInitialization(val withdrawalConfig: WithdrawalServiceConfig) {
    val irohaCreator = withdrawalConfig.iroha.creator
    val irohaKeypair =
        ModelUtil.loadKeypair(
            withdrawalConfig.iroha.pubkeyPath,
            withdrawalConfig.iroha.privkeyPath
        ).get()

    val irohaHost = withdrawalConfig.iroha.hostname
    val irohaPort = withdrawalConfig.iroha.port
    val irohaNetwork = IrohaNetworkImpl(irohaHost, irohaPort)

    /**
     * Init Iroha chain listener
     * @return Observable on Iroha sidechain events
     */
    private fun initIrohaChain(): Result<Observable<SideChainEvent.IrohaEvent>, Exception> {
        logger.info { "Init Iroha chain listener" }

        return IrohaChainListener(irohaHost, irohaPort, irohaCreator, irohaKeypair).getBlockObservable()
            .map { observable ->
                observable.flatMapIterable { IrohaChainHandler().parseBlock(it) }
            }
    }

    /**
     * Init Withdrawal Service
     */
    private fun initWithdrawalService(inputEvents: Observable<SideChainEvent.IrohaEvent>): WithdrawalService {
        logger.info { "Init Withdrawal Service" }

        return WithdrawalServiceImpl(withdrawalConfig, irohaKeypair, irohaNetwork, inputEvents)
    }

    private fun initEthConsumer(withdrawalService: WithdrawalService): Result<Unit, Exception> {
        logger.info { "Init Ether consumer" }

        return Result.of {
            val ethConsumer = EthConsumer(withdrawalConfig.ethereum)
            withdrawalService.output()
                .subscribe({
                    it.map {
                        ethConsumer.consume(it)
                    }.failure {
                        logger.error { it }
                    }
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
