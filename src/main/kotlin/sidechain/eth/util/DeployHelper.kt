package sidechain.eth.util

import config.EthereumConfig
import config.EthereumPasswords
import contract.BasicCoin
import contract.Master
import contract.Relay
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import java.math.BigInteger

/**
 * Helper class for contracts deploying
 */
class DeployHelper(ethereumConfig: EthereumConfig, ethereumPasswords: EthereumPasswords) {

    /** web3 service instance to communicate with Ethereum network */
    val web3 = Web3j.build(HttpService(ethereumConfig.url))

    /** credentials of ethereum user */
    val credentials by lazy {
        WalletUtils.loadCredentials(
            ethereumPasswords.credentialsPassword,
            ethereumConfig.credentialsPath
        )
    }

    /** Gas price */
    val gasPrice = BigInteger.valueOf(ethereumConfig.gasPrice)

    /** Max gas limit */
    val gasLimit = BigInteger.valueOf(ethereumConfig.gasLimit)

    /**
     * Sends given amount of ether from some predefined account to given account
     * @param amount amount of ether to send
     * @param to target account
     */
    fun sendEthereum(amount: BigInteger, to: String) {
        // get the next available nonce
        val ethGetTransactionCount = web3.ethGetTransactionCount(
            credentials.address, DefaultBlockParameterName.LATEST
        ).send()
        val nonce = ethGetTransactionCount.transactionCount

        // create our transaction
        val rawTransaction = RawTransaction.createTransaction(
            nonce,
            gasPrice,
            gasLimit,
            to,
            amount,
            ""
        )

        // sign & send our transaction
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
        val hexValue = Numeric.toHexString(signedMessage)
        web3.ethSendRawTransaction(hexValue).send()
    }

    /**
     * Deploy BasicCoin smart contract
     * @return token smart contract object
     */
    fun deployBasicCoinSmartContract(): BasicCoin {
        return contract.BasicCoin.deploy(
            web3,
            credentials,
            gasPrice,
            gasLimit,
            BigInteger.valueOf(1000),
            credentials.address
        ).send()
    }

    /**
     * Deploy master smart contract
     * @return master smart contract object
     */
    fun deployMasterSmartContract(): Master {
        return contract.Master.deploy(
            web3,
            credentials,
            gasPrice,
            gasLimit
        ).send()
    }

    /**
     * Deploy relay smart contract
     * @param master notary master account
     * @param tokens list of supported tokens
     * @return relay smart contract object
     */
    fun deployRelaySmartContract(master: String, tokens: List<String>): Relay {
        return contract.Relay.deploy(
            web3,
            credentials,
            gasPrice,
            gasLimit,
            master,
            tokens
        ).send()
    }

    /**
     * Deploy all smart contracts:
     * - master
     * - token
     * - relay
     */
    fun deployAll() {
        val token = deployBasicCoinSmartContract()
        val master = deployMasterSmartContract()

        val tokens = listOf(token.contractAddress)
        val relay = deployRelaySmartContract(master.contractAddress, tokens)

        println("Token contract address: $token")
        println("Master contract address: $master")
        println("Relay contract address: $relay")
    }
}
