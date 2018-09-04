package withdrawalservice

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.EthereumPasswords
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
 * @param withdrawalEthereumPasswords - passwords for ethereum withdrawal account
 */
class WithdrawalServiceInitialization(
    private val withdrawalConfig: WithdrawalServiceConfig,
    private val withdrawalEthereumPasswords: EthereumPasswords
) {

    init {
        logger.info {
            """Start withdrawal service initialization with configs:
                |iroha: ${withdrawalConfig.iroha.hostname}:${withdrawalConfig.iroha.port}""".trimMargin()
        }
    }

    private val irohaCreator = withdrawalConfig.iroha.creator
    private val irohaKeypair =
        ModelUtil.loadKeypair(
            withdrawalConfig.iroha.pubkeyPath,
            withdrawalConfig.iroha.privkeyPath
        ).get()

    private val irohaHost = withdrawalConfig.iroha.hostname
    private val irohaPort = withdrawalConfig.iroha.port
    private val irohaNetwork = IrohaNetworkImpl(irohaHost, irohaPort)

    /**
     * Init Iroha chain listener
     * @return Observable on Iroha sidechain events
     */
    private fun initIrohaChain(): Result<Observable<SideChainEvent.IrohaEvent>, Exception> {
        logger.info { "Init Iroha chain listener" }
        return IrohaChainListener(irohaHost, irohaPort, irohaCreator, irohaKeypair).getBlockObservable()
            .map { observable ->
                observable.flatMapIterable { block -> IrohaChainHandler().parseBlock(block) }
            }
    }

    /**
     * Init Withdrawal Service
     */
    private fun initWithdrawalService(inputEvents: Observable<SideChainEvent.IrohaEvent>): WithdrawalService {
        logger.info { "Init Withdrawal Service" }

        return WithdrawalServiceImpl(withdrawalConfig,  irohaKeypair, irohaNetwork, inputEvents)
    }

    private fun initEthConsumer(withdrawalService: WithdrawalService): Result<Unit, Exception> {
        logger.info { "Init Ether consumer" }

        return Result.of {
            val ethConsumer = EthConsumer(withdrawalConfig.ethereum, withdrawalEthereumPasswords)
            withdrawalService.output()
                .subscribe({
                    it.map { withdrawalEvent ->
                        ethConsumer.consume(withdrawalEvent)
                    }.failure { ex ->
                        logger.error("WithdrawalServiceOutputEvent", ex)
                    }
                }, { ex ->
                    logger.error("Withdrawal observable error", ex)
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
