package sidechain.eth.consumer

import config.EthereumConfig
import config.EthereumPasswords
import contract.Relay
import mu.KLogging
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.utils.Numeric
import sidechain.eth.util.DeployHelper
import vacuum.RelayVacuumConfig
import vacuum.executeVacuum
import withdrawalservice.WithdrawalServiceOutputEvent
import java.math.BigInteger

class EthConsumer(
    ethereumConfig: EthereumConfig,
    ethereumPasswords: EthereumPasswords,
    private val relayVacuumConfig: RelayVacuumConfig
) {
    private val deployHelper = DeployHelper(ethereumConfig, ethereumPasswords)
    fun consume(event: WithdrawalServiceOutputEvent) {
        logger.info { "Consumed eth event $event" }
        if (event is WithdrawalServiceOutputEvent.EthRefund) {
            logger.info {
                "Got proof:\n" +
                        "account ${event.proof.account}\n" +
                        "amount ${event.proof.amount}\n" +
                        "token ${event.proof.tokenContractAddress}\n" +
                        "iroha hash ${event.proof.irohaHash}\n" +
                        "relay ${event.proof.relay}\n"
            }

            // The first withdraw call
            val call = withdraw(event)
            // Here works next logic:
            // If the first call returns logs with size 2 then check if a destination address is equal to the address
            // from the second log
            // If its true then we start vacuum process
            for (log in call!!.logs) {
                if (log.topics.contains("0x33d1e0301846de1496df73b1da3d17c85b7266dd832d21e10ff21a1f143ef293")
                    && event.proof.account.toLowerCase() == "0x" + log.data.toLowerCase().subSequence(90, 130)
                ) {
                    executeVacuum(relayVacuumConfig).fold(
                        {
                            withdraw(event)
                        },
                        { ex ->
                            throw ex
                        }
                    )
                }
            }
        }
    }

    fun withdraw(event: WithdrawalServiceOutputEvent.EthRefund): TransactionReceipt? {
        val relay = Relay.load(
            event.proof.relay,
            deployHelper.web3,
            deployHelper.credentials,
            deployHelper.gasPrice,
            deployHelper.gasLimit
        )

        return relay.withdraw(
            event.proof.tokenContractAddress,
            BigInteger(event.proof.amount),
            event.proof.account,
            Numeric.hexStringToByteArray(event.proof.irohaHash),
            event.proof.v,
            event.proof.r,
            event.proof.s,
            relay.contractAddress
        ).send()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
