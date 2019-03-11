package com.d3.eth.notary.endpoint

import com.d3.commons.provider.WhiteListProvider
import com.d3.commons.registration.ETH_WHITE_LIST_KEY
import jp.co.soramitsu.iroha.java.QueryAPI

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
