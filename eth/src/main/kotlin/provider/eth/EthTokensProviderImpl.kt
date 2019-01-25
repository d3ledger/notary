package provider.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.QueryAPI
import mu.KLogging
import sidechain.iroha.util.getAccountDetails
import sidechain.iroha.util.getAssetPrecision

const val ETH_NAME = "ether"
const val ETH_PRECISION: Int = 18
const val ETH_ADDRESS = "0x0000000000000000000000000000000000000000"

/**
 * Implementation of [EthTokensProvider] with Iroha storage.
 *
 * @param queryAPI - iroha queries network layer
 * @param tokenStorageAccount - tokenStorageAccount that contains details
 * @param tokenSetterAccount - tokenSetterAccount that holds tokens in tokenStorageAccount account
 */
class EthTokensProviderImpl(
    private val queryAPI: QueryAPI,
    private val tokenStorageAccount: String,
    private val tokenSetterAccount: String
) : EthTokensProvider {

    init {
        logger.info { "Init token provider, storage: '$tokenStorageAccount', setter: '$tokenSetterAccount'" }
    }

    /**
     * Get all tokens. Returns EthreumAddress -> TokenName
     */
    override fun getTokens(): Result<Map<String, String>, Exception> {
        return getAccountDetails(
            queryAPI,
            tokenStorageAccount,
            tokenSetterAccount
        )
    }

    /**
     * Get precision of [name] asset in Iroha.
     */
    override fun getTokenPrecision(name: String): Result<Int, Exception> {
        return if (name == ETH_NAME)
            Result.of { ETH_PRECISION }
        else getAssetPrecision(
            queryAPI,
            "$name#ethereum"
        )
    }

    /**
     * Get token address of [name] asset. For ether returns 0x0000000000000000000000000000000000000000
     */
    override fun getTokenAddress(name: String): Result<String, Exception> {
        return if (name == ETH_NAME)
            Result.of { ETH_ADDRESS }
        else getAccountDetails(
            queryAPI,
            tokenStorageAccount,
            tokenSetterAccount
        ).map { tokens ->
            tokens.filterValues {
                it == name
            }.keys.first()
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
