package integration

import contract.BasicCoin
import contract.Master
import contract.Relay
import notary.CONFIG
import main.ConfigKeys
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigInteger

/**
 * Helper class for contracts deploying
 */
class DeployHelper {
    /** web3 service instance to communicate with Ethereum network */
    val web3 = Web3j.build(HttpService(CONFIG[ConfigKeys.testEthConnectionUrl]))

    /** credentials of ethereum user */
    val credentials =
            WalletUtils.loadCredentials("user", "deploy/ethereum/keys/user.key")

    /** Gas price */
    val gasPrice = BigInteger.ONE

    /** Max gas limit */
    val gasLimit = BigInteger.valueOf(999999)

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
