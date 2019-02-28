package notary.endpoint.eth

import jp.co.soramitsu.iroha.java.QueryAPI
import provider.WhiteListProvider

const val ETH_WHITE_LIST_KEY = "eth_whitelist"

/*
    White list provider for Ethereum services
 */
class EthWhiteListProvider(
    whiteListSetterAccount: String,
    queryAPI: QueryAPI
) : WhiteListProvider(
    whiteListSetterAccount, queryAPI,
    ETH_WHITE_LIST_KEY
)
