package com.d3.commons.provider

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.IrohaAPI
import com.d3.commons.model.IrohaCredential
import mu.KLogging
import com.d3.commons.notary.IrohaCommand
import com.d3.commons.notary.IrohaTransaction
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.consumer.IrohaConverter
import com.d3.commons.sidechain.iroha.util.ModelUtil

/*
    Provider that helps us to implement pub/sub mechanism in Iroha using account as an event source.
 */
class TriggerProvider(
    private val callerCredential: IrohaCredential,
    irohaAPI: IrohaAPI,
    private val triggerAccount: String
) {
    init {
        logger.info {
            "Init trigger provider with triggered account '$triggerAccount' and trigger caller account '${callerCredential.accountId}'"
        }
    }

    private val irohaConsumer = IrohaConsumerImpl(callerCredential, irohaAPI)

    /**
     * Triggers [triggerAccount] by setting details
     *
     * @param payload - some data to store
     * @return Result of detail setting process
     */
    fun trigger(payload: String): Result<Unit, Exception> {
        val utx = IrohaConverter.convert(triggerTx(payload))
        return irohaConsumer.send(utx)
            .map {
                logger.info { "$triggerAccount was triggered with payload $payload" }
                Unit
            }
    }

    /**
     * Creates transaction that may be used to trigger [triggerAccount] by setting details
     *
     * @param payload - some data to store
     * @return Iroha transaction full of triggering commands
     */
    fun triggerTx(payload: String): IrohaTransaction {
        return IrohaTransaction(
            irohaConsumer.creator,
            ModelUtil.getCurrentTime(),
            1,
            arrayListOf(
                IrohaCommand.CommandSetAccountDetail(
                    triggerAccount,
                    payload,
                    ""
                )
            )
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
