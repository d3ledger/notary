package notary.endpoint.eth

import model.IrohaCredential
import provider.WhiteListProvider
import registration.ETH_WHITE_LIST_KEY
import sidechain.iroha.consumer.IrohaNetwork

/*
    White list provider for Ethereum services
 */
class EthWhiteListProvider(
    whiteListSetterAccount: String,
    credential: IrohaCredential,
    irohaNetwork: IrohaNetwork
) : WhiteListProvider(
    whiteListSetterAccount, credential, irohaNetwork,
    ETH_WHITE_LIST_KEY
)
