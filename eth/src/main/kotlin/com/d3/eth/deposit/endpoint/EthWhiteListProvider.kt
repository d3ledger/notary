package com.d3.eth.deposit.endpoint

import com.d3.commons.provider.WhiteListProvider
import com.d3.commons.registration.ETH_WHITE_LIST_KEY
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper

/*
 * Whitelist provider for Ethereum services
 */
class EthWhiteListProvider(
    whiteListSetterAccount: String,
    queryHelper: IrohaQueryHelper
) : WhiteListProvider(
    whiteListSetterAccount,
    queryHelper,
    ETH_WHITE_LIST_KEY
)
