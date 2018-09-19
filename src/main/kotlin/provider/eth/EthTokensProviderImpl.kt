package provider.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.IrohaConfig
import jp.co.soramitsu.iroha.Keypair
import mu.KLogging
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.getAccountDetails
import sidechain.iroha.util.getAssetPrecision

/**
 * Implementation of [EthTokensProvider] with Iroha storage.
 *
 * @param irohaConfig - Iroha configuration
 * @param keypair - Iroha keypair to query
 * @param tokenStorageAccount - tokenStorageAccount that contains details
 * @param tokenSetterAccount - tokenSetterAccount that holds tokens in tokenStorageAccount account
 */
class EthTokensProviderImpl(
    private val irohaConfig: IrohaConfig,
    private val keypair: Keypair,
    private val tokenStorageAccount: String,
    private val tokenSetterAccount: String
) : EthTokensProvider {

    init {
        EthRelayProviderIrohaImpl.logger.info {
            "Init token provider, sotrage: '$tokenStorageAccount', setter: '$tokenSetterAccount'"
        }
    }

    private val irohaNetwork = IrohaNetworkImpl(irohaConfig.hostname, irohaConfig.port)
    private val irohaConsumer = IrohaConsumerImpl(irohaConfig.creator, irohaConfig)

    override fun getTokens(): Result<Map<String, EthTokenInfo>, Exception> {
        return getAccountDetails(
            irohaConfig,
            keypair,
            irohaNetwork,
            tokenStorageAccount,
            tokenSetterAccount
        )
            .map {
                it.mapValues { (_, name) ->
                    getAssetPrecision(
                        irohaConfig,
                        keypair,
                        irohaNetwork,
                        "$name#ethereum"
                    ).fold(
                        { precision ->
                            EthTokenInfo(name, precision)
                        },
                        { ex -> throw ex }
                    )
                }
            }
    }

    /**
     * Adds all given ERC20 tokens in Iroha
     * @param tokens - map of tokens (address->token info)
     * @return Result of operation
     */
    override fun addTokens(tokens: Map<String, EthTokenInfo>): Result<Unit, Exception> {
        logger.info { "ERC20 tokens to register $tokens" }
        return ModelUtil.registerERC20Tokens(tokens, tokenStorageAccount, irohaConsumer)
            .map { Unit }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
