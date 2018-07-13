package sidechain.eth.consumer

import mu.KLogging
import org.web3j.utils.Numeric
import util.eth.DeployHelper
import withdrawalservice.WithdrawalServiceOutputEvent

class EthConsumer {
    val deployHelper = DeployHelper()

    fun consume(event: WithdrawalServiceOutputEvent) {
        logger.info { "consumed eth event" }
        if (event is WithdrawalServiceOutputEvent.EthRefund) {
            if (event.proof != null) {
                logger.info {
                    "Got proof:\n" +
                            "account ${event.proof.account}\n" +
                            "amount ${event.proof.amount}\n" +
                            "token ${event.proof.tokenContractAddress}\n" +
                            "iroha_hash ${event.proof.iroha_hash}\n" +
                            "relay ${event.proof.relay}\n"
                }

                val relay = contract.Relay.load(
                        event.proof.relay,
                        deployHelper.web3,
                        deployHelper.credentials,
                        deployHelper.gasPrice,
                        deployHelper.gasLimit)

                relay.withdraw(
                        event.proof.tokenContractAddress,
                        event.proof.amount,
                        event.proof.account,
                        Numeric.hexStringToByteArray(event.proof.iroha_hash),
                        event.proof.v,
                        event.proof.r,
                        event.proof.s).sendAsync()
            }
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
