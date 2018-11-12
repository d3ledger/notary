package withdrawal.btc

import model.IrohaCredential
import provider.WhiteListProvider
import provider.btc.account.BTC_WHITE_LIST_KEY
import sidechain.iroha.consumer.IrohaNetwork

/*
    White list provider for Bitcoin services
 */
class BtcWhiteListProvider(
    whiteListSetterAccount: String,
    credential: IrohaCredential,
    irohaNetwork: IrohaNetwork
) : WhiteListProvider(
    whiteListSetterAccount, credential, irohaNetwork,
    BTC_WHITE_LIST_KEY
)
