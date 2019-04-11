package com.d3.btc.withdrawal.provider

import com.d3.btc.withdrawal.config.BtcWithdrawalConfig
import com.d3.btc.withdrawal.transaction.TransactionHelper
import com.d3.btc.withdrawal.transaction.WithdrawalDetails
import com.d3.btc.withdrawal.transaction.consensus.ConsensusDataStorage
import com.d3.btc.withdrawal.transaction.consensus.WithdrawalConsensus
import com.d3.commons.model.IrohaCredential
import com.d3.commons.notary.IrohaCommand
import com.d3.commons.notary.IrohaTransaction
import com.d3.commons.provider.NotaryPeerListProvider
import com.d3.commons.sidechain.iroha.BTC_CONSENSUS_DOMAIN
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.consumer.IrohaConverter
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomId
import com.d3.commons.util.hex
import com.d3.commons.util.irohaEscape
import com.d3.commons.util.toJson
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import kotlin.math.absoluteValue

@Component
class WithdrawalConsensusProvider(
    @Qualifier("consensusIrohaCredential")
    @Autowired private val consensusIrohaCredential: IrohaCredential,
    @Qualifier("consensusIrohaConsumer")
    @Autowired private val consensusIrohaConsumer: IrohaConsumer,
    @Autowired private val peerListProvider: NotaryPeerListProvider,
    @Autowired private val transactionHelper: TransactionHelper,
    @Autowired private val btcWithdrawalConfig: BtcWithdrawalConfig
) {
    /**
     * Creates consensus data and saves it in Iroha
     * @param withdrawalDetails - withdrawal details that will be used to create consensus
     */
    fun createConsensusData(withdrawalDetails: WithdrawalDetails): Result<Unit, Exception> {
        //Create consensus storage for withdrawal
        ConsensusDataStorage.create(withdrawalDetails)
        val consensusAccountName = withdrawalDetails.irohaFriendlyHashCode()
        val consensusAccountId = "$consensusAccountName@$BTC_CONSENSUS_DOMAIN"
        return transactionHelper.getAvailableUTXOHeight(
            btcWithdrawalConfig.bitcoin.confidenceLevel,
            withdrawalDetails.withdrawalTime
        ).map { availableHeight ->
            val withdrawalConsensus =
                WithdrawalConsensus(availableHeight, peerListProvider.getPeerList().size)
            /**
             * Another node may try to create the same account.
             * So this transaction may fall legally.
             */
            consensusIrohaConsumer.send(IrohaConverter.convert(createAccountTx(consensusAccountName)))
            withdrawalConsensus
        }.map { withdrawalConsensus ->
            consensusIrohaConsumer.send(
                IrohaConverter.convert(addConsensusDataTx(consensusAccountId, withdrawalConsensus))
            ).fold(
                {
                    logger.info(
                        "Consensus data $withdrawalConsensus has been " +
                                "successfully saved into $consensusAccountId account"
                    )
                }, { ex -> throw ex })
        }
    }

    /**
     * Creates account creation transaction.
     * This account will be used to store consensus data
     * @param consensusAccountName - account name where consensus data will be stored
     * @return well formed transaction
     */
    private fun createAccountTx(
        consensusAccountName: String
    ): IrohaTransaction {
        return IrohaTransaction(
            consensusIrohaCredential.accountId,
            ModelUtil.getCurrentTime(),
            1,
            arrayListOf(
                IrohaCommand.CommandCreateAccount(
                    consensusAccountName,
                    BTC_CONSENSUS_DOMAIN,
                    String.hex(consensusIrohaCredential.keyPair.public.encoded)
                )
            )
        )
    }

    /**
     * Creates consensus data addition transaction
     * @param consensusAccountId - account id, where consensus data will be stored
     * @param withdrawalConsensus - withdrawal consensus data
     * @return well formed transaction
     */
    private fun addConsensusDataTx(
        consensusAccountId: String,
        withdrawalConsensus: WithdrawalConsensus
    ): IrohaTransaction {
        return IrohaTransaction(
            consensusIrohaCredential.accountId,
            ModelUtil.getCurrentTime(),
            1,
            arrayListOf(
                IrohaCommand.CommandSetAccountDetail(
                    consensusAccountId,
                    String.getRandomId(),
                    withdrawalConsensus.toJson().irohaEscape()
                )
            )
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
