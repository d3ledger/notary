package util.eth

import config.ConfigKeys
import notary.CONFIG
import org.web3j.crypto.Hash
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.parity.Parity
import java.math.BigInteger

fun signUserData(to_sign: String): String {
    // TODO luckychess 26.06.2018 D3-100 find a way to produce correct signatures locally
    val deploy_helper = DeployHelper()
    val parity = Parity.build(HttpService(CONFIG[ConfigKeys.testEthConnectionUrl]))
    parity.personalUnlockAccount(deploy_helper.credentials.address, "user").send()
    return parity.ethSign(deploy_helper.credentials.address, to_sign).send().signature
}

fun hashToWithdraw(token_address: String, amount: BigInteger, account_address: String, iroha_hash: String): String {
    return Hash.sha3(token_address.replace("0x", "")
            + String.format("%064x", amount).replace("0x", "")
            + account_address.replace("0x", "")
            + iroha_hash.replace("0x", ""))
}
