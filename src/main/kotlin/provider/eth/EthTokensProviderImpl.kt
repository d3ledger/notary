package provider.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import config.IrohaConfig
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.getAccountDetails
import sidechain.iroha.util.getAssetPrecision

const val ETH_NAME = "ether"
const val ETH_PRECISION: Short = 18
const val ETH_ADDRESS = "0x0000000000000000000000000000000000000000"

/**
 * Implementation of [EthTokensProvider] with Iroha storage.
 *
 * @param credential - Iroha credential
 * @param tokenStorageAccount - tokenStorageAccount that contains details
 * @param tokenSetterAccount - tokenSetterAccount that holds tokens in tokenStorageAccount account
 * @param irohaNetwork - iroha network
 */
class EthTokensProviderImpl(
    private val credential: IrohaCredential,
    private val tokenStorageAccount: String,
    private val tokenSetterAccount: String,
    private val irohaNetwork: IrohaNetwork
) : EthTokensProvider {

    init {
        logger.info { "Init token provider, storage: '$tokenStorageAccount', setter: '$tokenSetterAccount'" }
    }

    /**
     * Get all tokens. Returns EthreumAddress -> TokenName
     */
    override fun getTokens(): Result<Map<String, String>, Exception> {
        return getAccountDetails(
            credential,
            irohaNetwork,
            tokenStorageAccount,
            tokenSetterAccount
        )
    }

    /**
     * Get precision of [name] asset in Iroha.
     */
    override fun getTokenPrecision(name: String): Result<Short, Exception> {
        return if (name == ETH_NAME)
            Result.of { ETH_PRECISION }
        else getAssetPrecision(
            credential,
            irohaNetwork,
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
            credential,
            irohaNetwork,
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
