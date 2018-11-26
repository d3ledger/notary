package integration.helper

import config.EthereumPasswords
import config.loadConfigs
import integration.TestConfig
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.utils.Numeric
import sidechain.eth.util.DeployHelper
import sidechain.eth.util.extractVRS
import sidechain.eth.util.hashToWithdraw
import sidechain.eth.util.signUserData
import java.math.BigInteger
import kotlin.test.assertEquals

class ContractTestHelper {
    private val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties")
    private val passwordConfig = loadConfigs("test", EthereumPasswords::class.java, "/eth/ethereum_password.properties")

    val deployHelper = DeployHelper(testConfig.ethereum, passwordConfig)

    val keypair = deployHelper.credentials.ecKeyPair
    val relayRegistry by lazy { deployHelper.deployRelayRegistrySmartContract() }
    val token by lazy { deployHelper.deployERC20TokenSmartContract() }
    val master by lazy { deployHelper.deployMasterSmartContract(relayRegistry.contractAddress) }
    val relay by lazy { deployHelper.deployRelaySmartContract(master.contractAddress) }

    private var addPeerCalls = BigInteger.ZERO

    val etherAddress = "0x0000000000000000000000000000000000000000"
    val defaultIrohaHash = Hash.sha3(String.format("%064x", BigInteger.valueOf(12345)))
    val defaultByteHash = Numeric.hexStringToByteArray(defaultIrohaHash.slice(2 until defaultIrohaHash.length))

    // ganache-cli ether custodian
    val accMain = deployHelper.credentials.address
    // some ganache-cli account
    val accGreen = testConfig.ethTestAccount

    data class sigsData(val vv: ArrayList<BigInteger>, val rr: ArrayList<ByteArray>, val ss: ArrayList<ByteArray>)

    fun prepareSignatures(amount: Int, keypairs: List<ECKeyPair>, toSign: String): sigsData {
        val vv = ArrayList<BigInteger>()
        val rr = ArrayList<ByteArray>()
        val ss = ArrayList<ByteArray>()

        for (i in 0 until amount) {
            val signature = signUserData(keypairs[i], toSign)
            val vrs = extractVRS(signature)
            vv.add(vrs.v)
            rr.add(vrs.r)
            ss.add(vrs.s)
        }
        return sigsData(vv, rr, ss)
    }

    fun sendAddPeer(address: String) {
        ++addPeerCalls
        master.addPeer(address).send()
        // addPeer return number of added peers
        assertEquals(addPeerCalls, master.peersCount().send())
    }

    fun transferTokensToMaster(amount: BigInteger) {
        token.transfer(master.contractAddress, amount, BigInteger.ZERO).send()
        assertEquals(amount, token.balanceOf(master.contractAddress).send())
    }

    /**
     * Withdraw assets
     * @param amount how much to withdraw
     * @return receipt of a transaction was sent
     */
    fun withdraw(amount: BigInteger): TransactionReceipt {
        val tokenAddress = token.contractAddress
        val to = accGreen

        val finalHash = hashToWithdraw(tokenAddress, amount.toString(), to, defaultIrohaHash, relay.contractAddress)
        val sigs = prepareSignatures(1, listOf(keypair), finalHash)

        return master.withdraw(
            tokenAddress,
            amount,
            to,
            defaultByteHash,
            sigs.vv,
            sigs.rr,
            sigs.ss,
            relay.contractAddress
        ).send()
    }

    /**
     * Withdraw assets
     * @param amount how much to withdraw
     * @param to account address
     * @return receipt of a transaction was sent
     */
    fun withdraw(amount: BigInteger, to: String): TransactionReceipt {
        val tokenAddress = token.contractAddress

        val finalHash = hashToWithdraw(tokenAddress, amount.toString(), to, defaultIrohaHash, relay.contractAddress)
        val sigs = prepareSignatures(1, listOf(keypair), finalHash)

        return master.withdraw(
            tokenAddress,
            amount,
            to,
            defaultByteHash,
            sigs.vv,
            sigs.rr,
            sigs.ss,
            relay.contractAddress
        ).send()
    }

    /**
     * Withdraw assets
     * @param amount how much to withdraw
     * @param to destination address
     * @param tokenAddress Ethereum address of ERC-20 token
     * @return receipt of a transaction was sent
     */
    fun withdraw(
        amount: BigInteger,
        to: String,
        tokenAddress: String
    ): TransactionReceipt {
        val finalHash = hashToWithdraw(tokenAddress, amount.toString(), to, defaultIrohaHash, relay.contractAddress)
        val sigs = prepareSignatures(1, listOf(keypair), finalHash)

        return master.withdraw(
            tokenAddress,
            amount,
            to,
            defaultByteHash,
            sigs.vv,
            sigs.rr,
            sigs.ss,
            relay.contractAddress
        ).send()
    }

    /**
     * Withdraw assets
     * @param amount how much to withdraw
     * @param to destination address
     * @param tokenAddress Ethereum address of ERC-20 token
     * @param fromMaster true if withdraw should proceeded from master contract
     * @return receipt of a transaction was sent
     */
    fun withdraw(
        amount: BigInteger,
        tokenAddress: String,
        to: String,
        fromMaster: Boolean
    ): TransactionReceipt {
        val finalHash = hashToWithdraw(tokenAddress, amount.toString(), to, defaultIrohaHash, relay.contractAddress)
        val sigs = prepareSignatures(1, listOf(keypair), finalHash)
        if (fromMaster) {
            return master.withdraw(
                tokenAddress,
                amount,
                to,
                defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                relay.contractAddress
            ).send()
        } else {
            addWhiteListToRelayRegistry(relay.contractAddress, listOf(to))
            return relay.withdraw(
                tokenAddress,
                amount,
                to,
                defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                relay.contractAddress
            ).send()
        }
    }

    /**
     * Save white list in the contract
     * @param relayAddress relay contract address
     * @param whiteList list of addresses allowed to withdraw
     * @return receipt of a transaction was sent
     */
    fun addWhiteListToRelayRegistry(
        relayAddress: String,
        whiteList: List<String>
    ): TransactionReceipt {
        return relayRegistry.addNewRelayAddress(relayAddress, whiteList).send()
    }

    fun sendEthereum(amount: BigInteger, to: String) {
        deployHelper.sendEthereum(amount, to)
    }

    fun sendERC20Token(contractAddress: String, amount: BigInteger, toAddress: String) {
        deployHelper.sendERC20(contractAddress, toAddress, amount)
    }

    fun getETHBalance(whoAddress: String): BigInteger {
        return deployHelper.getETHBalance(whoAddress)
    }

    fun getERC20TokenBalance(contractAddress: String, whoAddress: String): BigInteger {
        return deployHelper.getERC20Balance(contractAddress, whoAddress)
    }

    fun deployFailer(): String {
        return deployHelper.deployFailerContract().contractAddress
    }
}