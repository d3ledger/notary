package provider.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import model.IrohaCredential
import notary.IrohaCommand
import notary.IrohaTransaction
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.ModelUtil

// Class for creating session accounts. Theses accounts are used to store BTC public keys.
class BtcSessionProvider(
    private val credential: IrohaCredential,
    irohaNetwork: IrohaNetwork
) {
    private val irohaConsumer = IrohaConsumerImpl(credential, irohaNetwork)

    /**
     * Creates a special session account for notaries public key storage
     *
     * @param sessionId session identifier aka session account name
     * @return Result of account creation process
     */
    fun createPubKeyCreationSession(sessionId: String): Result<String, Exception> {
        return Result.of {
            IrohaTransaction(
                credential.accountId,
                ModelUtil.getCurrentTime(),
                1,
                arrayListOf(
                    IrohaCommand.CommandCreateAccount(
                        sessionId, "btcSession", credential.keyPair.publicKey().hex()
                    )
                )
            )
        }.flatMap { irohaTx ->
            val utx = IrohaConverterImpl().convert(irohaTx)
            irohaConsumer.sendAndCheck(utx)

        }
    }

}
