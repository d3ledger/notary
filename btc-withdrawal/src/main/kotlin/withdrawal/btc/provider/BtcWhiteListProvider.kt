package withdrawal.btc.provider

import jp.co.soramitsu.iroha.java.IrohaAPI
import model.IrohaCredential
import provider.WhiteListProvider
import provider.btc.account.BTC_WHITE_LIST_KEY

/*
    White list provider for Bitcoin services
 */
class BtcWhiteListProvider(
    whiteListSetterAccount: String,
    credential: IrohaCredential,
    irohaAPI: IrohaAPI
) : WhiteListProvider(
    whiteListSetterAccount, credential, irohaAPI,
    BTC_WHITE_LIST_KEY
)
