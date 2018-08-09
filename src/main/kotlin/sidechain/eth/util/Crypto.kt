package sidechain.eth.util

import config.EthereumConfig
import config.EthereumPasswords
import notary.endpoint.eth.AmountType
import org.web3j.crypto.Hash
import org.web3j.protocol.admin.Admin
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import java.math.BigInteger


/**
 * Signs user-provided data with predefined account deployed on local Parity node
 * @param toSign data to sign
 * @return signed data
 */
fun signUserData(ethereumConfig: EthereumConfig, ethereumPasswords: EthereumPasswords, toSign: String): String {
    // TODO luckychess 26.06.2018 D3-100 find a way to produce correct signatures locally
    val deployHelper = DeployHelper(ethereumConfig, ethereumPasswords)

    val admin = Admin.build(HttpService(ethereumConfig.url))
    admin.personalUnlockAccount(deployHelper.credentials.address, ethereumPasswords.credentialsPassword).send()
    // TODO: signalize about error if unlock fails

    return admin.ethSign(deployHelper.credentials.address, toSign).send().signature
}

// TODO: remove when config hell will be fixed
fun testSignUserData(url: String, address: String, toSign: String): String {
    val admin = Admin.build(HttpService(url))
    assert(admin.personalUnlockAccount(address, "user").send().accountUnlocked())
    return admin.ethSign(address, toSign).send().signature
}

/**
 * Calculates keccak-256 hash of several params concatenation. Params are:
 * @param tokenAddress Ethereum address of ERC-20 token (0x0000000000000000000000000000000000000000 for ether)
 * @param amount amount of token/ether to transfer
 * @param accountAddress address to transfer token/eth to
 * @param irohaHash hash of transaction in Iroha
 * @return keccak-256 hash of all provided fields
 */
fun hashToWithdraw(tokenAddress: String, amount: AmountType, accountAddress: String, irohaHash: String): String {
    return Hash.sha3(
        tokenAddress.replace("0x", "")
                + String.format("%064x", BigInteger(amount)).replace("0x", "")
                + accountAddress.replace("0x", "")
                + irohaHash.replace("0x", "")
    )
}

/**
 * Data class which stores signature splitted into components
 * @param v v component of signature
 * @param r r component of signature
 * @param s s component of signature
 */
data class VRS(val v: BigInteger, val r: ByteArray, val s: ByteArray)

/**
 * Extracts VRS-signature from string-encoded signature
 * @param signature string-encoded signature
 * @return VRS object
 */
fun extractVRS(signature: String): VRS {
    val r = Numeric.hexStringToByteArray(signature.substring(2, 66))
    val s = Numeric.hexStringToByteArray(signature.substring(66, 130))
    var v = signature.substring(130, 132).toBigInteger(16)
    if (v == BigInteger.valueOf(0) || v == BigInteger.valueOf(1)) {
        v += BigInteger.valueOf(27)
    }
    return VRS(v, r, s)
}
