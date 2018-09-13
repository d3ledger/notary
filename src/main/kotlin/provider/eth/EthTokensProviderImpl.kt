package provider.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.IrohaConfig
import jp.co.soramitsu.iroha.Keypair
import mu.KLogging
import notary.IrohaCommand
import notary.IrohaTransaction
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverterImpl
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
                    ).fold(
                        { precision ->
                            EthTokenInfo(name, precision)
                        },
                        { ex -> throw ex }
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

    /**
     * Registers all given ERC20 tokens in Iroha
     * @param tokens - map of tokens (address->token info)
     * @return Result of operation
     */
    override fun registerTokens(tokens: Map<String, EthTokenInfo>): Result<Unit, Exception> {
        logger.info { "tokens registration $tokens" }
        return Result.of {
            //2 commands per token:create asset and set detail
            val commands = ArrayList<IrohaCommand>(tokens.size * 2)
            tokens.forEach { ethWallet, ethTokenInfo ->
                commands.add(
                    IrohaCommand.CommandCreateAsset(
                        ethTokenInfo.name,
                        "ethereum",
                        ethTokenInfo.precision.toShort()
                    )
                )
                commands.add(
                    IrohaCommand.CommandSetAccountDetail(
                        notaryIrohaAccount,
                        ethWallet,
                        ethTokenInfo.name
                    )
                )
            }
            IrohaTransaction(tokenStorageAccount, commands)
        }.flatMap { irohaTx ->
            val utx = IrohaConverterImpl().convert(irohaTx)
            irohaConsumer.sendAndCheck(utx)
        }.map { Unit }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
