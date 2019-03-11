package com.d3.btc.provider.generation

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import jp.co.soramitsu.iroha.java.IrohaAPI
import com.d3.commons.model.IrohaCredential
import com.d3.commons.notary.IrohaCommand
import com.d3.commons.notary.IrohaTransaction
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.consumer.IrohaConverter
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.hex

private const val BTC_SESSION_DOMAIN = "btcSession"
const val ADDRESS_GENERATION_TIME_KEY = "addressGenerationTime"
const val ADDRESS_GENERATION_NODE_ID_KEY = "nodeId"

// Class for creating session accounts. Theses accounts are used to store BTC public keys.
class BtcSessionProvider(
    private val credential: IrohaCredential,
    irohaAPI: IrohaAPI
) {
    private val irohaConsumer = IrohaConsumerImpl(credential, irohaAPI)

    /**
     * Creates a special session account for notaries public key storage
     *
     * @param sessionId session identifier aka session account name
     * @param nodeId - node id
     * @return Result of account creation process
     */
    fun createPubKeyCreationSession(sessionId: String, nodeId: String): Result<String, Exception> {
        return Result.of {
            createPubKeyCreationSessionTx(sessionId, nodeId)
        }.flatMap { irohaTx ->
            val utx = IrohaConverter.convert(irohaTx)
            irohaConsumer.send(utx)
        }
    }

    /**
     * Creates a transaction that may be used to create special session account for notaries public key storage
     *
     * @param sessionId - session identifier aka session account name
     * @param nodeId - node id
     * @return Iroha transaction full of session creation commands
     */
    fun createPubKeyCreationSessionTx(sessionId: String, nodeId: String): IrohaTransaction {
        return IrohaTransaction(
            credential.accountId,
            ModelUtil.getCurrentTime(),
            1,
            arrayListOf(
                //Creating session account
                IrohaCommand.CommandCreateAccount(
                    sessionId, BTC_SESSION_DOMAIN, String.hex(credential.keyPair.public.encoded)
                ),
                //Setting address generation time
                IrohaCommand.CommandSetAccountDetail(
                    "$sessionId@$BTC_SESSION_DOMAIN",
                    ADDRESS_GENERATION_TIME_KEY,
                    System.currentTimeMillis().toString()
                ),
                //Setting node id
                IrohaCommand.CommandSetAccountDetail(
                    "$sessionId@$BTC_SESSION_DOMAIN",
                    ADDRESS_GENERATION_NODE_ID_KEY,
                    nodeId
                )
            )
        )
    }
}
