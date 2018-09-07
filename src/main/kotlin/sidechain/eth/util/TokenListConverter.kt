package sidechain.eth.util

import provider.eth.EthTokenInfo

val ETH_PRECISION = 18

fun findInTokens(tokenToFind: String, tokens: MutableMap<String, EthTokenInfo>): String {
    tokens["0x0000000000000000000000000000000000000000"] = EthTokenInfo("ether", ETH_PRECISION)
    for (coin in tokens) {
        if (coin.value.name == tokenToFind) {
            return coin.key
        }
    }
    throw Exception("Not supported token type")
}
