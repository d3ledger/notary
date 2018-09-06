package provider.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.IrohaConfig
import jp.co.soramitsu.iroha.Keypair
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil.setAccountDetail
import sidechain.iroha.util.getAccountDetails
import sidechain.iroha.util.getAssetPrecision

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

    override fun getTokens(): Result<Map<String, EthTokenInfo>, Exception> {
        return getAccountDetails(
            irohaConfig,
            keypair,
            irohaNetwork,
            notaryIrohaAccount,
            tokenStorageAccount
        )
            .map {
                it.mapValues { (_, name) ->
                    getAssetPrecision(
                        irohaConfig,
                        keypair,
                        irohaNetwork,
                        "$name#ethereum"
                    )
                        .fold(
                            { EthTokenInfo(name, it) },
                            { throw it }
                        )
                }
            }
    }

    override fun addToken(ethWallet: String, tokenInfo: EthTokenInfo): Result<Unit, Exception> {
        return setAccountDetail(
            irohaConsumer,
            tokenStorageAccount,
            notaryIrohaAccount,
            ethWallet,
            tokenInfo.name
        ).map { Unit }
    }
}
