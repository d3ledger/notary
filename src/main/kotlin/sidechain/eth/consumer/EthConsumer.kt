package sidechain.eth.consumer

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.success
import mu.KLogging
import org.web3j.utils.Numeric
import sidechain.eth.util.DeployHelper
import withdrawalservice.WithdrawalServiceOutputEvent

class EthConsumer {
    private val deployHelper = DeployHelper()

    fun consume(event: Result<WithdrawalServiceOutputEvent, Exception>) {
        event.success {
            logger.info { "consumed eth event" }
            val eventSuccess = event.get()
            if (eventSuccess is WithdrawalServiceOutputEvent.EthRefund) {
                logger.info {
                    "Got proof:\n" +
                            "account ${eventSuccess.proof.account}\n" +
                            "amount ${eventSuccess.proof.amount}\n" +
                            "token ${eventSuccess.proof.tokenContractAddress}\n" +
                            "iroha hash ${eventSuccess.proof.irohaHash}\n" +
                            "relay ${eventSuccess.proof.relay}\n"
                }

                val relay = contract.Relay.load(
                    eventSuccess.proof.relay,
                    deployHelper.web3,
                    deployHelper.credentials,
                    deployHelper.gasPrice,
                    deployHelper.gasLimit
                )

                relay.withdraw(
                    eventSuccess.proof.tokenContractAddress,
                    eventSuccess.proof.amount,
                    eventSuccess.proof.account,
                    Numeric.hexStringToByteArray(eventSuccess.proof.irohaHash),
                    eventSuccess.proof.v,
                    eventSuccess.proof.r,
                    eventSuccess.proof.s
                ).sendAsync()
            }
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
