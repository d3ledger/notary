package com.d3.btc.withdrawal.provider

import com.d3.btc.provider.account.BTC_WHITE_LIST_KEY
import com.d3.commons.provider.WhiteListProvider
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper

/*
    White list provider for Bitcoin services
 */
class BtcWhiteListProvider(
    whiteListSetterAccount: String,
    queryHelper: IrohaQueryHelper
) : WhiteListProvider(
    whiteListSetterAccount,
    queryHelper,
    BTC_WHITE_LIST_KEY
)
