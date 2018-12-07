package provider.btc.generation

import com.github.kittinunf.result.Result
import model.IrohaCredential
import sidechain.iroha.consumer.IrohaConsumerImpl
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
        return ModelUtil.createAccount(
            irohaConsumer,
            sessionId,
            "btcSession",
            credential.keyPair.public
        )
    }

}
