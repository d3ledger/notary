package notary.endpoint.eth

import jp.co.soramitsu.iroha.java.IrohaAPI
import model.IrohaCredential
import provider.WhiteListProvider
import registration.ETH_WHITE_LIST_KEY

/*
    White list provider for Ethereum services
 */
class EthWhiteListProvider(
    whiteListSetterAccount: String,
    credential: IrohaCredential,
    irohaAPI: IrohaAPI
) : WhiteListProvider(
    whiteListSetterAccount, credential, irohaAPI,
    ETH_WHITE_LIST_KEY
)
