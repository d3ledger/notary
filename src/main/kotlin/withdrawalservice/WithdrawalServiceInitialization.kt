package withdrawalservice

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.EthereumPasswords
import io.reactivex.Observable
import model.IrohaCredential
import mu.KLogging
import sidechain.SideChainEvent
import sidechain.eth.consumer.EthConsumer
import sidechain.iroha.IrohaChainHandler
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.consumer.IrohaNetworkImpl
import vacuum.RelayVacuumConfig

/**
 * @param withdrawalConfig - configuration for withdrawal service
 * @param withdrawalEthereumPasswords - passwords for ethereum withdrawal account
 */
class WithdrawalServiceInitialization(
    private val withdrawalConfig: WithdrawalServiceConfig,
    private val credential: IrohaCredential,
    private val withdrawalEthereumPasswords: EthereumPasswords,
    private val relayVacuumConfig: RelayVacuumConfig
) {

    private val irohaHost = withdrawalConfig.iroha.hostname
    private val irohaPort = withdrawalConfig.iroha.port
    private val irohaNetwork = IrohaNetworkImpl(irohaHost, irohaPort)

    /**
     * Init Iroha chain listener
     * @return Observable on Iroha sidechain events
     */
    private fun initIrohaChain(): Result<Observable<SideChainEvent.IrohaEvent>, Exception> {
        logger.info { "Init Iroha chain listener" }
        return IrohaChainListener(irohaHost, irohaPort, credential).getBlockObservable()
            .map { observable ->
                observable.flatMapIterable { block -> IrohaChainHandler().parseBlock(block) }
            }
    }

    /**
     * Init Withdrawal Service
     */
    private fun initWithdrawalService(inputEvents: Observable<SideChainEvent.IrohaEvent>): WithdrawalService {
        logger.info { "Init Withdrawal Service" }

        return WithdrawalServiceImpl(withdrawalConfig, credential, irohaNetwork, inputEvents)
    }

    private fun initEthConsumer(withdrawalService: WithdrawalService): Result<Unit, Exception> {
        logger.info { "Init Ether withdrawal consumer" }

        return Result.of {
            val ethConsumer = EthConsumer(withdrawalConfig.ethereum, withdrawalEthereumPasswords, relayVacuumConfig)
            withdrawalService.output()
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe(
                    { res ->
                        res.map { withdrawalEvents ->
                            withdrawalEvents.map { event ->
                                ethConsumer.consume(event)
                            }
                        }.failure { ex ->
                            logger.error("Cannot consume withdrawal event", ex)
                        }
                    }, { ex ->
                        logger.error("Withdrawal observable error", ex)
                    }
                )
            Unit
        }
    }

    fun init(): Result<Unit, Exception> {
        logger.info {
            "Start withdrawal service init with iroha at ${withdrawalConfig.iroha.hostname}:${withdrawalConfig.iroha.port}"
        }
        return initIrohaChain()
            .map { initWithdrawalService(it) }
            .flatMap { initEthConsumer(it) }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
