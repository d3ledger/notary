package sidechain.eth.util

import config.EthereumConfig
import config.EthereumPasswords
import contract.*
import mu.KLogging
import okhttp3.*
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import java.math.BigDecimal
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

    /** transaction manager */
    val rawTransactionManager = RawTransactionManager(web3, credentials)

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
        val transfer = Transfer(web3, rawTransactionManager)
        val transactionHash =
            transfer.sendFunds(to, BigDecimal(amount), Convert.Unit.WEI, gasPrice, gasLimit).send().transactionHash
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
        val relayRegistry = contract.RelayRegistry.deploy(
            web3,
            credentials,
            gasPrice,
            gasLimit
        ).send()
        logger.info { "Relay Registry smart contract ${relayRegistry.contractAddress} was deployed" }
        return relayRegistry
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

    fun deployFailerContract(): Failer {
        val failer = Failer.deploy(web3, credentials, gasPrice, gasLimit).send()
        logger.info { "Failer smart contract ${failer.contractAddress} was deployed" }
        return failer
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
     * @return user balance
     */
    fun getERC20Balance(tokenAddress: String, whoAddress: String): BigInteger {
        val token = contract.BasicCoin.load(tokenAddress, web3, credentials, gasPrice, gasLimit)
        return token.balanceOf(whoAddress).send()
    }

    /**
     * Get ETH balance
     * @param whoAddress - user address to check
     * @return user balance
     */
    fun getETHBalance(whoAddress: String): BigInteger {
        return web3.ethGetBalance(whoAddress, DefaultBlockParameterName.LATEST).send().balance
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
