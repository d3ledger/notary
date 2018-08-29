package provider.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import config.IrohaConfig
import jp.co.soramitsu.iroha.Keypair
import notary.IrohaCommand
import notary.IrohaTransaction
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.util.ModelUtil


// Class for creating session accounts. Theses accounts are used to store BTC public keys.
class BtcSessionProvider(
    val irohaConfig: IrohaConfig,
    private val registrationAccount: String,
    private val keypair: Keypair
) {
    private val irohaConsumer = IrohaConsumerImpl(irohaConfig)
    /**
     * Creates a special session account for notaries public key storage
     *
     * @param sessionId session identifier aka session account name
     * @return Result of account creation process
     */
    fun createPubKeyCreationSession(sessionId: String): Result<String, Exception> {
        return Result.of {
            IrohaTransaction(
                registrationAccount,
                ModelUtil.getCurrentTime(),
                1,
                arrayListOf(
                    IrohaCommand.CommandCreateAccount(
                        sessionId, "btcSession", keypair.publicKey().hex()
                    )
                )
            )
        }.flatMap { irohaTx ->
            val utx = IrohaConverterImpl().convert(irohaTx)
            irohaConsumer.sendAndCheck(utx)
        }
    }
}
