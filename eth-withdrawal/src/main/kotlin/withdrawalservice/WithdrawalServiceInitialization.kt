package withdrawalservice

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.EthereumPasswords
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import jp.co.soramitsu.iroha.java.IrohaAPI
import model.IrohaCredential
import mu.KLogging
import sidechain.SideChainEvent
import sidechain.eth.consumer.EthConsumer
import sidechain.iroha.IrohaChainHandler
import sidechain.iroha.IrohaChainListener
import vacuum.RelayVacuumConfig

/**
 * @param withdrawalConfig - configuration for withdrawal service
 * @param withdrawalEthereumPasswords - passwords for ethereum withdrawal account
 */
class WithdrawalServiceInitialization(
    private val withdrawalConfig: WithdrawalServiceConfig,
    private val credential: IrohaCredential,
    private val irohaAPI: IrohaAPI,
    private val withdrawalEthereumPasswords: EthereumPasswords,
    private val relayVacuumConfig: RelayVacuumConfig
) {
    private val irohaHost = withdrawalConfig.iroha.hostname
    private val irohaPort = withdrawalConfig.iroha.port

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

        return WithdrawalServiceImpl(withdrawalConfig, credential, irohaAPI, inputEvents)
    }

    private fun initEthConsumer(withdrawalService: WithdrawalService): Result<Unit, Exception> {
        logger.info { "Init Ether withdrawal consumer" }

        return Result.of {
            val ethConsumer = EthConsumer(
                withdrawalConfig.ethereum,
                withdrawalEthereumPasswords,
                relayVacuumConfig
            )
            withdrawalService.output()
                .subscribeOn(Schedulers.newThread())
                .subscribe(
                    { res ->
                        res.map { withdrawalEvents ->
                            withdrawalEvents.map { event ->
                                val transactionReceipt = ethConsumer.consume(event)
                                // TODO: Add subtraction of assets from master account in Iroha in 'else'
                                if (transactionReceipt == null || transactionReceipt.status == FAILED_STATUS) {
                                    withdrawalService.returnIrohaAssets(event)
                                }
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
            .map { WithdrawalServiceEndpoint(withdrawalConfig.port) }
            .map { Unit }
    }

    /**
     * Logger
     */
    companion object : KLogging() {
        private const val FAILED_STATUS = "0x0"
    }
}
