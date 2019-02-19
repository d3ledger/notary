package com.d3.btc.withdrawal.provider

import com.d3.btc.provider.account.BTC_WHITE_LIST_KEY
import jp.co.soramitsu.iroha.java.QueryAPI
import provider.WhiteListProvider

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
