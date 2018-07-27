package integration

import config.EthereumPasswords
import config.TestConfig
import config.loadConfigs
import contract.BasicCoin
import contract.Master
import contract.Relay
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.crypto.Hash
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.exceptions.TransactionException
import org.web3j.utils.Numeric.hexStringToByteArray
import sidechain.eth.util.DeployHelper
import sidechain.eth.util.hashToWithdraw
import sidechain.eth.util.signUserData
import java.math.BigInteger

/**
 * Class for Ethereum sidechain infrastructure deployment and communication.
 */
class ContractsTest {
    private val testConfig = loadConfigs("test", TestConfig::class.java)
    private val passwordConfig = loadConfigs("test", EthereumPasswords::class.java, "/ethereum_password.properties")

    private val deployHelper = DeployHelper(testConfig.ethereum, passwordConfig)

    private lateinit var token: BasicCoin
    private lateinit var master: Master
    private lateinit var relay: Relay

    private var addPeerCalls: Int = 0

    // some ropsten account
    private val accGreen = "0x93668c6b9b8b9b9ea2393ab689e413cdd1e3440f"

    private fun sendAddPeer(address: String) {
        ++addPeerCalls
        val addPeer = master.addPeer(address).send()
        // addPeer call produces 2 events
        // first event is amount of peers after new peer was added
        // second one is address of added peer
        // events should be aligned to 64 hex digits and prefixed with 0x
        assertEquals(2, addPeer.logs.size)
        assertEquals("0x" + String.format("%064x", addPeerCalls), addPeer.logs[0].data)
        assertEquals(
            "0x" + "0".repeat(24) +
                    address.slice(2 until address.length),
            addPeer.logs[1].data
        )
        Thread.sleep(120_000)
    }

    private fun transferTokensToMaster(amount: BigInteger) {
        token.transfer(master.contractAddress, amount).send()
        //TODO: find a better way to wait
        Thread.sleep(300_000)
        assertEquals(amount, token.balanceOf(master.contractAddress).send())
    }

    private fun withdraw(
        amount: BigInteger,
        irohaHash: String = Hash.sha3(String.format("%064x", BigInteger.valueOf(12345))),
        tokenAddress: String = token.contractAddress,
        to: String = accGreen,
        fromMaster: Boolean = true
    ) {
        val finalHash = hashToWithdraw(tokenAddress, amount.toString(), to, irohaHash)

        val signature = signUserData(testConfig.ethereum, passwordConfig, finalHash)
        val r = hexStringToByteArray(signature.substring(2, 66))
        val s = hexStringToByteArray(signature.substring(66, 130))
        val v = signature.substring(130, 132).toBigInteger(16)

        val vv = ArrayList<BigInteger>()
        vv.add(v)
        val rr = ArrayList<ByteArray>()
        rr.add(r)
        val ss = ArrayList<ByteArray>()
        ss.add(s)

        val byteHash = hexStringToByteArray(irohaHash.slice(2 until irohaHash.length))
        if (fromMaster) {
            master.withdraw(
                tokenAddress,
                amount,
                to,
                byteHash,
                vv,
                rr,
                ss
            ).send()
        } else {
            relay.withdraw(
                tokenAddress,
                amount,
                to,
                byteHash,
                vv,
                rr,
                ss
            ).send()
        }
        Thread.sleep(120_000)
    }

    @BeforeEach
    fun setup() {
        token = deployHelper.deployERC20TokenSmartContract()
        master = deployHelper.deployMasterSmartContract()
        Thread.sleep(120_000)
        relay = deployHelper.deployRelaySmartContract(master.contractAddress, listOf(token.contractAddress))
        addPeerCalls = 0
    }

    /**
     * @given master account deployed
     * @when transfer 300_000_000 WEIs to master account
     * @then balance of master account increased by 300_000_000
     */
    @Test
    fun canAcceptEther() {
        val initialBalance =
            deployHelper.web3.ethGetBalance(master.contractAddress, DefaultBlockParameterName.LATEST).send().balance
        deployHelper.sendEthereum(BigInteger.valueOf(300_000_000), master.contractAddress)
        // have to wait some time until balance will be updated
        Thread.sleep(120_000)
        assertEquals(
            initialBalance + BigInteger.valueOf(300_000_000),
            deployHelper.web3.ethGetBalance(
                master.contractAddress,
                DefaultBlockParameterName.LATEST
            ).send().balance
        )
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master
     * @then call to withdraw succeeded
     */
    @Test
    fun singleCorrectSignatureTokenTest() {
        sendAddPeer(deployHelper.credentials.address)
        transferTokensToMaster(BigInteger.valueOf(5))
        withdraw(BigInteger.valueOf(1))
        assertEquals(BigInteger.valueOf(4), token.balanceOf(master.contractAddress).send())
    }

    /**
     * @given deployed master and token contracts
     * @when two peers added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master
     * @then call to withdraw fails
     */
    @Test
    fun singleNotEnoughSignaturesTokenTest() {
        sendAddPeer(deployHelper.credentials.address)
        sendAddPeer(accGreen)
        transferTokensToMaster(BigInteger.valueOf(5))
        Assertions.assertThrows(TransactionException::class.java) { withdraw(BigInteger.valueOf(1)) }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5000 WEI transferred to master,
     * request to withdraw 10000 WEI is sent to master
     * @then call to withdraw fails
     */
    @Test
    fun notEnoughEtherTest() {
        sendAddPeer(deployHelper.credentials.address)
        deployHelper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
        // have to wait some time until balance will be updated
        Thread.sleep(120_000)
        Assertions.assertThrows(TransactionException::class.java) {
            withdraw(
                BigInteger.valueOf(10000),
                tokenAddress = "0x0000000000000000000000000000000000000000",
                to = accGreen
            )
        }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 10 tokens is sent to master
     * @then call to withdraw fails
     */
    @Test
    fun notEnoughTokensTest() {
        sendAddPeer(deployHelper.credentials.address)
        transferTokensToMaster(BigInteger.valueOf(5))
        Assertions.assertThrows(TransactionException::class.java) { withdraw(BigInteger.valueOf(10)) }
    }

    /**
     * @given deployed master contract
     * @when one peer added to master, 5000 Wei transferred to master,
     * request to withdraw 1000 Wei is sent to master
     * @then call to withdraw succeeded
     */
    @Test
    fun singleCorrectSignatureEtherTestMaster() {
        val initialBalance =
            deployHelper.web3.ethGetBalance(accGreen, DefaultBlockParameterName.LATEST).send().balance
        sendAddPeer(deployHelper.credentials.address)
        deployHelper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
        // have to wait some time until balance will be updated
        Thread.sleep(120_000)
        withdraw(
            BigInteger.valueOf(1000),
            tokenAddress = "0x0000000000000000000000000000000000000000",
            to = accGreen
        )
        assertEquals(
            BigInteger.valueOf(4000),
            deployHelper.web3.ethGetBalance(master.contractAddress, DefaultBlockParameterName.LATEST).send().balance
        )
        assertEquals(
            initialBalance + BigInteger.valueOf(1000),
            deployHelper.web3.ethGetBalance(accGreen, DefaultBlockParameterName.LATEST).send().balance
        )
    }

    /**
     * @given deployed master and relay contracts
     * @when one peer added to master, 5000 Wei transferred to master,
     * request to withdraw 1000 Wei is sent to relay
     * @then call to withdraw succeeded
     */
    @Test
    fun singleCorrectSignatureEtherTestRelay() {
        val initialBalance =
            deployHelper.web3.ethGetBalance(accGreen, DefaultBlockParameterName.LATEST).send().balance
        sendAddPeer(deployHelper.credentials.address)
        deployHelper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
        // have to wait some time until balance will be updated
        Thread.sleep(120_000)
        withdraw(
            BigInteger.valueOf(1000), tokenAddress = "0x0000000000000000000000000000000000000000",
            fromMaster = false, to = accGreen
        )
        assertEquals(
            BigInteger.valueOf(4000),
            deployHelper.web3.ethGetBalance(master.contractAddress, DefaultBlockParameterName.LATEST).send().balance
        )
        assertEquals(
            initialBalance + BigInteger.valueOf(1000),
            deployHelper.web3.ethGetBalance(accGreen, DefaultBlockParameterName.LATEST).send().balance
        )
    }

    /**
     * @given deployed master and relay contracts
     * @when one peer added to master, 5000 Wei transferred to master,
     * request to withdraw 1000 Wei is sent to relay, destination address is also set to relay
     * @then call to withdraw succeeded
     */
    @Test
    fun singleCorrectSignatureEtherTestRelayToRelay() {
        val initialBalance =
            deployHelper.web3.ethGetBalance(relay.contractAddress, DefaultBlockParameterName.LATEST).send().balance
        sendAddPeer(deployHelper.credentials.address)
        deployHelper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
        // have to wait some time until balance will be updated
        Thread.sleep(120_000)
        withdraw(
            BigInteger.valueOf(1000), tokenAddress = "0x0000000000000000000000000000000000000000",
            fromMaster = false, to = relay.contractAddress
        )
        assertEquals(
            BigInteger.valueOf(4000),
            deployHelper.web3.ethGetBalance(master.contractAddress, DefaultBlockParameterName.LATEST).send().balance
        )
        assertEquals(
            initialBalance + BigInteger.valueOf(1000),
            deployHelper.web3.ethGetBalance(relay.contractAddress, DefaultBlockParameterName.LATEST).send().balance
        )
    }

    /**
     * @given deployed master and token contracts
     * @when 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master
     * @then withdraw attempt fails
     */
    @Test
    fun noPeersWithdraw() {
        transferTokensToMaster(BigInteger.valueOf(5))
        Assertions.assertThrows(TransactionException::class.java) { withdraw(BigInteger.valueOf(1)) }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master with r-array larger than v and s
     * @then withdraw attempt fails
     */
    @Test
    fun differentVRS() {
        sendAddPeer(deployHelper.credentials.address)
        transferTokensToMaster(BigInteger.valueOf(5))

        val amount = BigInteger.valueOf(1)
        val irohaHash = Hash.sha3(String.format("%064x", BigInteger.valueOf(12345)))
        val finalHash = hashToWithdraw(token.contractAddress, amount.toString(), accGreen, irohaHash)

        val signature = signUserData(testConfig.ethereum, passwordConfig, finalHash)
        val r = hexStringToByteArray(signature.substring(2, 66))
        val s = hexStringToByteArray(signature.substring(66, 130))
        val v = signature.substring(130, 132).toBigInteger(16)

        val vv = ArrayList<BigInteger>()
        vv.add(v)
        val rr = ArrayList<ByteArray>()
        rr.add(r)
        val ss = ArrayList<ByteArray>()
        ss.add(s)
        rr.add(s)

        val byteHash = hexStringToByteArray(irohaHash.slice(2 until irohaHash.length))

        Assertions.assertThrows(TransactionException::class.java) {
            master.withdraw(
                token.contractAddress,
                amount,
                accGreen,
                byteHash,
                vv,
                rr,
                ss
            ).send()
        }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master with valid signature repeated twice
     * @then call to withdraw fails
     * //TODO: withdraw should pass successfully until amount of duplicated and other invalid signatures <= f
     */
    @Test
    fun sameSignatures() {
        sendAddPeer(deployHelper.credentials.address)
        transferTokensToMaster(BigInteger.valueOf(5))

        val amount = BigInteger.valueOf(1)
        val irohaHash = Hash.sha3(String.format("%064x", BigInteger.valueOf(12345)))
        val finalHash = hashToWithdraw(token.contractAddress, amount.toString(), accGreen, irohaHash)

        val signature = signUserData(testConfig.ethereum, passwordConfig, finalHash)
        val r = hexStringToByteArray(signature.substring(2, 66))
        val s = hexStringToByteArray(signature.substring(66, 130))
        val v = signature.substring(130, 132).toBigInteger(16)

        val vv = ArrayList<BigInteger>()
        vv.add(v)
        vv.add(v)
        val rr = ArrayList<ByteArray>()
        rr.add(r)
        rr.add(r)
        val ss = ArrayList<ByteArray>()
        ss.add(s)
        ss.add(s)

        val byteHash = hexStringToByteArray(irohaHash.slice(2 until irohaHash.length))

        Assertions.assertThrows(TransactionException::class.java) {
            master.withdraw(
                token.contractAddress,
                amount,
                accGreen,
                byteHash,
                vv,
                rr,
                ss
            ).send()
        }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master with invalid signature
     * @then call to withdraw fails
     */
    @Test
    fun invalidSignature() {
        sendAddPeer(deployHelper.credentials.address)
        transferTokensToMaster(BigInteger.valueOf(5))

        val amount = BigInteger.valueOf(1)
        val irohaHash = Hash.sha3(String.format("%064x", BigInteger.valueOf(12345)))
        val finalHash = hashToWithdraw(token.contractAddress, amount.toString(), accGreen, irohaHash)

        val signature = signUserData(testConfig.ethereum, passwordConfig, finalHash)
        val r = hexStringToByteArray(signature.substring(2, 66))
        val s = hexStringToByteArray(signature.substring(66, 130))
        // let's corrupt first byte of s
        s[0] = s[0].inc()
        val v = signature.substring(130, 132).toBigInteger(16)

        val vv = ArrayList<BigInteger>()
        vv.add(v)
        val rr = ArrayList<ByteArray>()
        rr.add(r)
        val ss = ArrayList<ByteArray>()
        ss.add(s)

        val byteHash = hexStringToByteArray(irohaHash.slice(2 until irohaHash.length))

        Assertions.assertThrows(TransactionException::class.java) {
            master.withdraw(
                token.contractAddress,
                amount,
                accGreen,
                byteHash,
                vv,
                rr,
                ss
            ).send()
        }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master twice
     * @then second call to withdraw failed
     */
    @Test
    fun usedHashTest() {
        sendAddPeer(deployHelper.credentials.address)
        transferTokensToMaster(BigInteger.valueOf(5))
        withdraw(BigInteger.valueOf(1))
        assertEquals(BigInteger.valueOf(4), token.balanceOf(master.contractAddress).send())
        withdraw(BigInteger.valueOf(1))
        assertEquals(BigInteger.valueOf(4), token.balanceOf(master.contractAddress).send())
    }

    /**
     * @given deployed master contract
     * @when AddPeer called twice with different addresses
     * @then both calls succeeded
     */
    @Test
    fun addPeerTest() {
        sendAddPeer(deployHelper.credentials.address)
        sendAddPeer(accGreen)
    }

    /**
     * @given deployed master contract
     * @when AddPeer called twice with same addresses
     * @then second call fails
     */
    @Test
    fun addSamePeer() {
        sendAddPeer(accGreen)
        Assertions.assertThrows(TransactionException::class.java) { master.addPeer(accGreen).send() }
    }
}
