package util.eth

import config.ConfigKeys
import notary.CONFIG
import org.web3j.crypto.Hash
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.parity.Parity
import java.math.BigInteger

fun signUserData(to_sign: String): String {
    // TODO luckychess 26.06.2018 D3-100 find a way to produce correct signatures locally
    val deployHelper = DeployHelper()
    val parity = Parity.build(HttpService(CONFIG[ConfigKeys.testEthConnectionUrl]))
    parity.personalUnlockAccount(deployHelper.credentials.address, "user").send()
    return parity.ethSign(deployHelper.credentials.address, to_sign).send().signature
}

fun hashToWithdraw(tokenAddress: String, amount: BigInteger, accountAddress: String, irohaHash: String): String {
    return Hash.sha3(
        tokenAddress.replace("0x", "")
                + String.format("%064x", amount).replace("0x", "")
                + accountAddress.replace("0x", "")
                + irohaHash.replace("0x", "")
    )
}
