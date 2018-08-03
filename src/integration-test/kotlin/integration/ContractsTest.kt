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
import sidechain.eth.util.extractVRS
import sidechain.eth.util.hashToWithdraw
import sidechain.eth.util.testSignUserData
import java.math.BigInteger

/**
 * Class for Ethereum sidechain infrastructure deployment and communication.
 */
class ContractsTest {
    private val testConfig = loadConfigs("test", TestConfig::class.java)
    private val passwordConfig = loadConfigs("test", EthereumPasswords::class.java, "/ethereum_password.properties")

    private val deployHelper = DeployHelper(testConfig.ethereum, passwordConfig, true)

    private lateinit var token: BasicCoin
    private lateinit var master: Master
    private lateinit var relay: Relay

    private var addPeerCalls: Int = 0

    // ganache-cli ether custodian
    private val accMain = "0x6826d84158e516f631bbf14586a9be7e255b2d23"
    // some ganache-cli account
    private val accGreen = "0x82e0b6cc1ea0d0b91f5fc86328b8e613bdaf72e8"

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
    }

    private fun transferTokensToMaster(amount: BigInteger) {
        token.transfer(master.contractAddress, amount).send()
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
        val signature = testSignUserData("http://127.0.0.1:8545", accMain, finalHash)
        val vrs = extractVRS(signature)

        val vv = ArrayList<BigInteger>()
        vv.add(vrs.v)
        val rr = ArrayList<ByteArray>()
        rr.add(vrs.r)
        val ss = ArrayList<ByteArray>()
        ss.add(vrs.s)

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
    }

    @BeforeEach
    fun setup() {
        token = deployHelper.deployERC20TokenSmartContract()
        master = deployHelper.deployMasterSmartContract(listOf(token.contractAddress))
        relay = deployHelper.deployRelaySmartContract(master.contractAddress)
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
        sendAddPeer(accMain)
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
        sendAddPeer(accMain)
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
        sendAddPeer(accMain)
        deployHelper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
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
        sendAddPeer(accMain)
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
        sendAddPeer(accMain)
        deployHelper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
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
        sendAddPeer(accMain)
        deployHelper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
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
        sendAddPeer(accMain)
        deployHelper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
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
        sendAddPeer(accMain)
        transferTokensToMaster(BigInteger.valueOf(5))

        val amount = BigInteger.valueOf(1)
        val irohaHash = Hash.sha3(String.format("%064x", BigInteger.valueOf(12345)))
        val finalHash = hashToWithdraw(token.contractAddress, amount.toString(), accGreen, irohaHash)

        val signature = testSignUserData("http://127.0.0.1:8545", accMain, finalHash)
        val vrs = extractVRS(signature)

        val vv = ArrayList<BigInteger>()
        vv.add(vrs.v)
        val rr = ArrayList<ByteArray>()
        rr.add(vrs.r)
        val ss = ArrayList<ByteArray>()
        ss.add(vrs.s)
        rr.add(vrs.s)

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
        sendAddPeer(accMain)
        transferTokensToMaster(BigInteger.valueOf(5))

        val amount = BigInteger.valueOf(1)
        val irohaHash = Hash.sha3(String.format("%064x", BigInteger.valueOf(12345)))
        val finalHash = hashToWithdraw(token.contractAddress, amount.toString(), accGreen, irohaHash)

        val signature = testSignUserData("http://127.0.0.1:8545", accMain, finalHash)
        val vrs = extractVRS(signature)

        val vv = ArrayList<BigInteger>()
        vv.add(vrs.v)
        vv.add(vrs.v)
        val rr = ArrayList<ByteArray>()
        rr.add(vrs.r)
        rr.add(vrs.r)
        val ss = ArrayList<ByteArray>()
        ss.add(vrs.s)
        ss.add(vrs.s)

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
        sendAddPeer(accMain)
        transferTokensToMaster(BigInteger.valueOf(5))

        val amount = BigInteger.valueOf(1)
        val irohaHash = Hash.sha3(String.format("%064x", BigInteger.valueOf(12345)))
        val finalHash = hashToWithdraw(token.contractAddress, amount.toString(), accGreen, irohaHash)

        val signature = testSignUserData("http://127.0.0.1:8545", accMain, finalHash)
        val vrs = extractVRS(signature)

        // let's corrupt first byte of s
        vrs.s[0] = vrs.s[0].inc()

        val vv = ArrayList<BigInteger>()
        vv.add(vrs.v)
        val rr = ArrayList<ByteArray>()
        rr.add(vrs.r)
        val ss = ArrayList<ByteArray>()
        ss.add(vrs.s)

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
        sendAddPeer(accMain)
        transferTokensToMaster(BigInteger.valueOf(5))
        withdraw(BigInteger.valueOf(1))
        assertEquals(BigInteger.valueOf(4), token.balanceOf(master.contractAddress).send())
        Assertions.assertThrows(TransactionException::class.java) { withdraw(BigInteger.valueOf(1)) }
    }

    /**
     * @given deployed master contract
     * @when AddPeer called twice with different addresses
     * @then both calls succeeded
     */
    @Test
    fun addPeerTest() {
        sendAddPeer(accMain)
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
