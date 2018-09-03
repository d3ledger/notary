package provider.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.IrohaConfig
import jp.co.soramitsu.iroha.Keypair
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil.setAccountDetail
import sidechain.iroha.util.getAccountDetails

/**
 * Implementation of [EthTokensProvider] with Iroha storage.
 *
 * @param irohaConfig - Iroha configuration
 * @param keypair - Iroha keypair to query
 * @param notaryIrohaAccount - notaryIrohaAccount that contains details
 * @param tokenStorageAccount - tokenStorageAccount that holds tokens in notaryIrohaAccount account
 */
class EthTokensProviderImpl(
    private val irohaConfig: IrohaConfig,
    private val keypair: Keypair,
    private val notaryIrohaAccount: String,
    private val tokenStorageAccount: String
) : EthTokensProvider {
    private val irohaNetwork = IrohaNetworkImpl(irohaConfig.hostname, irohaConfig.port)
    private val irohaConsumer = IrohaConsumerImpl(irohaConfig)

    override fun getTokens(): Result<Map<String, String>, Exception> {
        val tokens = getAccountDetails(
            irohaConfig,
            keypair,
            irohaNetwork,
            notaryIrohaAccount,
            tokenStorageAccount
        )
        return tokens
    }

    override fun addToken(ethWallet: String, tokenName: String): Result<Unit, Exception> {
        return setAccountDetail(
            irohaConsumer,
            tokenStorageAccount,
            notaryIrohaAccount,
            ethWallet,
            tokenName
        ).map { Unit }
    }
}
