package sidechain.eth.util

import config.EthereumConfig
import config.EthereumPasswords
import org.web3j.crypto.Hash
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.parity.Parity
import java.math.BigInteger

/**
 * Signs user-provided data with predefined account deployed on local Parity node
 * @param toSign data to sign
 * @return signed data
 */
fun signUserData(ethereumConfig: EthereumConfig, ethereumPasswords: EthereumPasswords, toSign: String): String {
    // TODO luckychess 26.06.2018 D3-100 find a way to produce correct signatures locally
    val deployHelper = DeployHelper(ethereumConfig, ethereumPasswords)
    val parity = Parity.build(HttpService(ethereumConfig.url))
    parity.personalUnlockAccount(
        deployHelper.credentials.address,
        ethereumPasswords.credentialsPassword
    ).send()
    return parity.ethSign(deployHelper.credentials.address, toSign).send().signature
}

/**
 * Calculates keccak-256 hash of several params concatenation. Params are:
 * @param tokenAddress Ethereum address of ERC-20 token (0x0000000000000000000000000000000000000000 for ether)
 * @param amount amount of token/ether to transfer
 * @param accountAddress address to transfer token/eth to
 * @param irohaHash hash of transaction in Iroha
 * @return keccak-256 hash of all provided fields
 */
fun hashToWithdraw(tokenAddress: String, amount: BigInteger, accountAddress: String, irohaHash: String): String {
    return Hash.sha3(
        tokenAddress.replace("0x", "")
                + String.format("%064x", amount).replace("0x", "")
                + accountAddress.replace("0x", "")
                + irohaHash.replace("0x", "")
    )
}
