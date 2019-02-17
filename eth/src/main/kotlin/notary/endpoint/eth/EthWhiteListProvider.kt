package notary.endpoint.eth

import jp.co.soramitsu.iroha.java.QueryAPI
import provider.WhiteListProvider
import registration.ETH_WHITE_LIST_KEY

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
