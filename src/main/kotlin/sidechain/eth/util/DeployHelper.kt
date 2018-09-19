package sidechain.eth.util

import config.EthereumConfig
import config.EthereumPasswords
import contract.BasicCoin
import contract.Master
import contract.Relay
import contract.RelayRegistry
import mu.KLogging
import okhttp3.*
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import java.math.BigInteger

/**
 * Authenticator class for basic access authentication
 * @param ethereumPasswords config with Ethereum node credentials
 */
class BasicAuthenticator(private val ethereumPasswords: EthereumPasswords) : Authenticator {
    override fun authenticate(route: Route, response: Response): Request {
        val credential = Credentials.basic(ethereumPasswords.nodeLogin, ethereumPasswords.nodePassword)
        return response.request().newBuilder().header("Authorization", credential).build()
    }
}

/**
 * Helper class for contracts deploying
 * @param ethereumConfig config with Ethereum network parameters
 * @param ethereumPasswords config with Ethereum passwords
 * @param etherTest is class used for contract tests or not (TODO: remove after config rework)
 */
class DeployHelper(ethereumConfig: EthereumConfig, ethereumPasswords: EthereumPasswords) {

    val url: String = ethereumConfig.url
    val web3: Web3j

    init {
        val builder = OkHttpClient().newBuilder()
        builder.authenticator(BasicAuthenticator(ethereumPasswords))
        web3 = Web3j.build(HttpService(ethereumConfig.url, builder.build(), false))

    }

    /** credentials of ethereum user */
    val credentials by lazy {
        WalletUtils.loadCredentials(ethereumPasswords.credentialsPassword, ethereumConfig.credentialsPath)
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
        val transactionHash = web3.ethSendRawTransaction(hexValue).send().transactionHash
        logger.info("ETH $amount were sent to $to; tx hash $transactionHash")
    }

    /**
     * Deploy ERC20 token smart contract
     * @return token smart contract object
     */
    fun deployERC20TokenSmartContract(): BasicCoin {
        val tokenContract = contract.BasicCoin.deploy(
            web3,
            credentials,
            gasPrice,
            gasLimit,
            BigInteger.valueOf(Long.MAX_VALUE),
            credentials.address
        ).send()
        logger.info { "ERC20 token smart contract ${tokenContract.contractAddress} was deployed" }
        return tokenContract
    }

    /**
     * Deploy relay registry smart contract
     * @return master smart contract object
     */
    fun deployRelayRegistrySmartContract(): RelayRegistry {
        return contract.RelayRegistry.deploy(
                web3,
                credentials,
                gasPrice,
                gasLimit
        ).send()
    }

    /**
     * Deploy master smart contract
     * @return master smart contract object
     */
    fun deployMasterSmartContract(relayRegistry: String): Master {
        val master = contract.Master.deploy(
                web3,
                credentials,
                gasPrice,
                gasLimit,
                relayRegistry
        ).send()
        logger.info { "Master smart contract ${master.contractAddress} was deployed" }
        return master
    }

    /**
     * Deploy relay smart contract
     * @param master notary master account
     * @return relay smart contract object
     */
    fun deployRelaySmartContract(master: String): Relay {
        val replay = contract.Relay.deploy(
            web3,
            credentials,
            gasPrice,
            gasLimit,
            master
        ).send()
        logger.info { "Relay smart contract ${replay.contractAddress} was deployed" }
        return replay
    }

    /**
     * Send ERC20 tokens
     * @param tokenAddress - address of token smart contract
     * @param toAddress - address transfer to
     * @param amount - amount of tokens
     */
    fun sendERC20(tokenAddress: String, toAddress: String, amount: BigInteger) {
        val token = contract.BasicCoin.load(tokenAddress, web3, credentials, gasPrice, gasLimit)
        token.transfer(toAddress, amount).send()
        logger.info { "ERC20 $amount with address $tokenAddress were sent to $toAddress" }
    }

    /**
     * Get ERC20 balance
     * @param tokenAddress - address of token smart contract
     * @param whoAddress - user address to check
     */
    fun getERC20Balance(tokenAddress: String, whoAddress: String): BigInteger {
        val token = contract.BasicCoin.load(tokenAddress, web3, credentials, gasPrice, gasLimit)
        return token.balanceOf(whoAddress).send()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
