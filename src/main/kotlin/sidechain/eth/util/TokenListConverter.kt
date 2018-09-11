package sidechain.eth.util

import provider.eth.EthTokenInfo

val ETH_PRECISION: Short = 18

fun findInTokens(tokenToFind: String, tokens: MutableMap<String, EthTokenInfo>): String {
    tokens["0x0000000000000000000000000000000000000000"] = EthTokenInfo("ether", ETH_PRECISION)
    for (coin in tokens) {
        if (coin.value.name == tokenToFind) {
            return coin.key
        }
    }
    throw Exception("Not supported token type")
}

fun getPrecision(token: String, tokens: MutableMap<String, EthTokenInfo>): Short {
    tokens["0x0000000000000000000000000000000000000000"] = EthTokenInfo("ether", ETH_PRECISION)
    for (coin in tokens) {
        if (coin.value.name == token) {
            return coin.value.precision
        }
    }
    throw Exception("Not supported token type")
}
