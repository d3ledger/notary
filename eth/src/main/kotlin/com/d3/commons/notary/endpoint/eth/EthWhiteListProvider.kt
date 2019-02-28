package com.d3.commons.notary.endpoint.eth

import jp.co.soramitsu.iroha.java.QueryAPI
import com.d3.commons.provider.WhiteListProvider
import com.d3.commons.registration.ETH_WHITE_LIST_KEY

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
