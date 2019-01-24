package withdrawal.btc.provider

import jp.co.soramitsu.iroha.java.QueryAPI
import provider.WhiteListProvider
import provider.btc.account.BTC_WHITE_LIST_KEY

/*
    White list provider for Bitcoin services
 */
class BtcWhiteListProvider(
    whiteListSetterAccount: String,
    queryAPI: QueryAPI
) : WhiteListProvider(
    whiteListSetterAccount, queryAPI,
    BTC_WHITE_LIST_KEY
)
