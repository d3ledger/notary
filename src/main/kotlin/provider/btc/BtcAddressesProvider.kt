package provider.btc

import com.github.kittinunf.result.Result
import config.IrohaConfig
import jp.co.soramitsu.iroha.Keypair
import org.bitcoinj.core.TransactionOutput
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.getAccountDetails

class BtcAddressesProvider(
    private val irohaConfig: IrohaConfig,
    private val keypair: Keypair,
    private val notaryIrohaAccount: String
) {
    private val irohaNetwork = IrohaNetworkImpl(irohaConfig.hostname, irohaConfig.port)
    /**
     * Get all registered btc addresses
     * @return map full of registered btc addresses (btc address -> iroha account name)
     */
    fun getAddresses(): Result<Map<String, String>, Exception> {
        return getAccountDetails(
            irohaConfig,
            keypair,
            irohaNetwork,
            notaryIrohaAccount,
            irohaConfig.creator
        )
    }

    fun getRegisteredAccounts(outputs: List<TransactionOutput>): List<String> {
        getAddresses().fold({ addresses ->
            return addresses.filter { address ->
                outputs.any { txOutput ->
                    txOutput.scriptPubKey.getToAddress(txOutput.params).toBase58() == address.key
                }
            }.map { address -> address.value }
        }, { ex ->
            throw ex
        })
    }
}
