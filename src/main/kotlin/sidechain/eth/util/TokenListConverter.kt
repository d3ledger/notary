package sidechain.eth.util

fun findInTokens(tokenToFind: String, tokens: MutableMap<String, String>): String {
    tokens["0x0000000000000000000000000000000000000000"] = "ether"
    for (coin in tokens) {
        if (coin.value == tokenToFind) {
            return coin.key
        }
    }
    throw Exception("Not supported token type")
}
