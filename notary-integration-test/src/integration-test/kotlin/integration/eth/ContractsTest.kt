package integration.eth

import config.EthereumPasswords
import config.loadConfigs
import contract.BasicCoin
import contract.Master
import contract.Relay
import contract.RelayRegistry
import integration.TestConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.exceptions.TransactionException
import org.web3j.utils.Numeric.hexStringToByteArray
import sidechain.eth.util.DeployHelper
import sidechain.eth.util.extractVRS
import sidechain.eth.util.hashToWithdraw
import sidechain.eth.util.signUserData
import java.math.BigInteger

/**
 * Class for Ethereum sidechain infrastructure deployment and communication.
 */
class ContractsTest {

    private val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties")
    private val passwordConfig = loadConfigs("test", EthereumPasswords::class.java, "/eth/ethereum_password.properties")
    private val deployHelper = DeployHelper(testConfig.ethereum, passwordConfig)
    private val keypair = deployHelper.credentials.ecKeyPair
    private lateinit var relayRegistry: RelayRegistry
    private lateinit var token: BasicCoin
    private lateinit var master: Master
    private lateinit var relay: Relay

    private var addPeerCalls = BigInteger.ZERO

    val etherAddress = "0x0000000000000000000000000000000000000000"
    val defaultIrohaHash = Hash.sha3(String.format("%064x", BigInteger.valueOf(12345)))
    val defaultByteHash = hexStringToByteArray(defaultIrohaHash.slice(2 until defaultIrohaHash.length))

    // ganache-cli ether custodian
    private val accMain = deployHelper.credentials.address
    // some ganache-cli account
    private val accGreen = testConfig.ethTestAccount

    data class sigsData(val vv: ArrayList<BigInteger>, val rr: ArrayList<ByteArray>, val ss: ArrayList<ByteArray>)

    private fun prepareSignatures(amount: Int, keypairs: List<ECKeyPair>, toSign: String): sigsData {
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

    private fun sendAddPeer(address: String) {
        ++addPeerCalls
        master.addPeer(address).send()
        // addPeer return number of added peers
        assertEquals(addPeerCalls, master.peersCount().send())
    }

    private fun transferTokensToMaster(amount: BigInteger) {
        token.transfer(master.contractAddress, amount).send()
        assertEquals(amount, token.balanceOf(master.contractAddress).send())
    }

    /**
     * Withdraw assets
     * @param amount how much to withdraw
     */
    private fun withdraw(amount: BigInteger) {
        val tokenAddress = token.contractAddress
        val to = accGreen

        val finalHash = hashToWithdraw(tokenAddress, amount.toString(), to, defaultIrohaHash, relay.contractAddress)
        val sigs = prepareSignatures(1, listOf(keypair), finalHash)

        master.withdraw(
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
     */
    private fun withdraw(to: String, amount: BigInteger) {
        val tokenAddress = token.contractAddress

        val finalHash = hashToWithdraw(tokenAddress, amount.toString(), to, defaultIrohaHash, relay.contractAddress)
        val sigs = prepareSignatures(1, listOf(keypair), finalHash)

        master.withdraw(
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
     * @param tokenAddress list of addresses allowed to withdraw
     * @param to destination address
     */
    private fun withdraw(
        amount: BigInteger,
        tokenAddress: String,
        to: String
    ): TransactionReceipt? {
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
     * @param tokenAddress list of addresses allowed to withdraw
     * @param to destination address
     * @param fromMaster true if withdraw should proceeded from master contract
     */
    private fun withdraw(
        amount: BigInteger,
        tokenAddress: String,
        to: String,
        fromMaster: Boolean
    ) {
        val finalHash = hashToWithdraw(tokenAddress, amount.toString(), to, defaultIrohaHash, relay.contractAddress)
        val sigs = prepareSignatures(1, listOf(keypair), finalHash)
        if (fromMaster) {
            master.withdraw(
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
            relay.withdraw(
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
     */
    private fun addWhiteListToRelayRegistry(
        relayAddress: String,
        whiteList: List<String>
    ) {
        relayRegistry.addNewRelayAddress(relayAddress, whiteList).send()
    }

    @BeforeEach
    fun setup() {
        token = deployHelper.deployERC20TokenSmartContract()
        relayRegistry = deployHelper.deployRelayRegistrySmartContract()
        master = deployHelper.deployMasterSmartContract(relayRegistry.contractAddress)
        relay = deployHelper.deployRelaySmartContract(master.contractAddress)
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
        master.disableAddingNewPeers().send()
        master.addToken(token.contractAddress).send()
        transferTokensToMaster(BigInteger.valueOf(5))
        addWhiteListToRelayRegistry(Keys.getAddress(keypair), listOf(accGreen))
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
        master.disableAddingNewPeers().send()
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
        master.disableAddingNewPeers().send()
        deployHelper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
        val call =
            withdraw(
                BigInteger.valueOf(10000),
                etherAddress,
                accGreen
            )
        assertEquals(
            "0x" + "0".repeat(24) +
                    etherAddress.slice(2 until etherAddress.length),
            call!!.logs[0].data.subSequence(0, 66)
        )
        assertEquals(accGreen, "0x" + call.logs[0].data.subSequence(90, 130))
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
        master.disableAddingNewPeers().send()
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
        master.disableAddingNewPeers().send()
        deployHelper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
        addWhiteListToRelayRegistry(Keys.getAddress(keypair), listOf(accGreen))
        withdraw(
            BigInteger.valueOf(1000),
            etherAddress,
            accGreen
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
        master.disableAddingNewPeers().send()
        deployHelper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
        withdraw(
            BigInteger.valueOf(1000), etherAddress,
            accGreen, false
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
        master.disableAddingNewPeers().send()
        deployHelper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
        withdraw(
            BigInteger.valueOf(1000), etherAddress,
            relay.contractAddress, false
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
        master.disableAddingNewPeers().send()
        transferTokensToMaster(BigInteger.valueOf(5))

        val amount = BigInteger.valueOf(1)
        val finalHash =
            hashToWithdraw(token.contractAddress, amount.toString(), accGreen, defaultIrohaHash, relay.contractAddress)
        val keypair = DeployHelper(testConfig.ethereum, passwordConfig).credentials.ecKeyPair

        val sigs = prepareSignatures(1, listOf(keypair), finalHash)
        sigs.rr.add(sigs.rr.first())

        Assertions.assertThrows(TransactionException::class.java) {
            master.withdraw(
                token.contractAddress,
                amount,
                accGreen,
                defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                relay.contractAddress
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
        master.disableAddingNewPeers().send()
        transferTokensToMaster(BigInteger.valueOf(5))

        val amount = BigInteger.valueOf(1)
        val finalHash =
            hashToWithdraw(token.contractAddress, amount.toString(), accGreen, defaultIrohaHash, relay.contractAddress)
        val sigs = prepareSignatures(2, listOf(keypair, keypair), finalHash)

        Assertions.assertThrows(TransactionException::class.java) {
            master.withdraw(
                token.contractAddress,
                amount,
                accGreen,
                defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                relay.contractAddress
            ).send()
        }
    }

    /**
     * @given deployed master contract
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master with two valid signatures:
     * one from added peer and another one from some unknown peer
     * @then call to withdraw fails
     * //TODO: withdraw should pass successfully until amount of duplicated and other invalid signatures <= f
     */
    @Test
    fun wrongPeerSignature() {
        sendAddPeer(accMain)
        master.disableAddingNewPeers().send()
        transferTokensToMaster(BigInteger.valueOf(5))

        val amount = BigInteger.valueOf(1)
        val finalHash =
            hashToWithdraw(token.contractAddress, amount.toString(), accGreen, defaultIrohaHash, relay.contractAddress)
        val sigs = prepareSignatures(2, listOf(keypair, Keys.createEcKeyPair()), finalHash)

        Assertions.assertThrows(TransactionException::class.java) {
            master.withdraw(
                token.contractAddress,
                amount,
                accGreen,
                defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                relay.contractAddress
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
        master.disableAddingNewPeers().send()
        transferTokensToMaster(BigInteger.valueOf(5))

        val amount = BigInteger.valueOf(1)
        val finalHash =
            hashToWithdraw(token.contractAddress, amount.toString(), accGreen, defaultIrohaHash, relay.contractAddress)

        val sigs = prepareSignatures(1, listOf(keypair), finalHash)
        sigs.ss.first()[0] = sigs.ss.first()[0].inc()

        Assertions.assertThrows(TransactionException::class.java) {
            master.withdraw(
                token.contractAddress,
                amount,
                accGreen,
                defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                relay.contractAddress
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
        master.disableAddingNewPeers().send()
        master.addToken(token.contractAddress).send()
        addWhiteListToRelayRegistry(Keys.getAddress(keypair), listOf(accGreen))
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

    /**
     * @given deployed master contract
     * @when addToken called twice with different addresses
     * @then both calls succeeded, both tokens are added
     */
    @Test
    fun addTokenTest() {
        val fakeTokenAddress = "0xf230b790e05390fc8295f4d3f60332c93bed42d1"
        master.addToken(token.contractAddress).send()
        master.addToken(fakeTokenAddress).send()
        val res = master.tokens.send()
        assertEquals(2, res.size)
        assertEquals(token.contractAddress, res[0])
        assertEquals(fakeTokenAddress, res[1])
    }

    /**
     * @given deployed master contract
     * @when addToken called twice with same addresses
     * @then second call throws exception
     */
    @Test
    fun addSameTokensTest() {
        master.addToken(token.contractAddress).send()
        Assertions.assertThrows(TransactionException::class.java) { master.addToken(token.contractAddress).send() }
    }

    /**
     * @given deployed master and relay contracts, 100000 Wei is sent to relay
     * @when sendToMaster of relay contract for Ether is called
     * @then relay contract has 0 Ether, master contract has 100000 Wei
     */
    @Test
    fun vacuumEtherTest() {
        deployHelper.sendEthereum(BigInteger.valueOf(100_000), relay.contractAddress)
        assertEquals(
            BigInteger.valueOf(0),
            deployHelper.web3.ethGetBalance(master.contractAddress, DefaultBlockParameterName.LATEST).send().balance
        )
        assertEquals(
            BigInteger.valueOf(100_000),
            deployHelper.web3.ethGetBalance(relay.contractAddress, DefaultBlockParameterName.LATEST).send().balance
        )
        relay.sendToMaster(etherAddress).send()
        assertEquals(
            BigInteger.valueOf(100_000),
            deployHelper.web3.ethGetBalance(master.contractAddress, DefaultBlockParameterName.LATEST).send().balance
        )
        assertEquals(
            BigInteger.valueOf(0),
            deployHelper.web3.ethGetBalance(relay.contractAddress, DefaultBlockParameterName.LATEST).send().balance
        )
    }

    /**
     * @given deployed master, relay and token contracts, addToken with token contract address is called,
     * 987654 tokens is sent to relay
     * @when sendToMaster of relay contract for token address is called
     * @then relay contract has 0 tokens, master contract has 987654 tokens
     */
    @Test
    fun vacuumTokenTest() {
        master.addToken(token.contractAddress).send()
        token.transfer(relay.contractAddress, BigInteger.valueOf(987_654)).send()
        assertEquals(BigInteger.valueOf(0), token.balanceOf(master.contractAddress).send())
        assertEquals(BigInteger.valueOf(987_654), token.balanceOf(relay.contractAddress).send())
        relay.sendToMaster(token.contractAddress).send()
        assertEquals(BigInteger.valueOf(987_654), token.balanceOf(master.contractAddress).send())
        assertEquals(BigInteger.valueOf(0), token.balanceOf(relay.contractAddress).send())
    }

    /**
     * @given deployed master, relay and token contracts, addToken never called,
     * 987654 tokens is sent to relay
     * @when sendToMaster of relay contract for token address is called
     * @then sendToMaster call fails
     */
    @Test
    fun vacuumInvalidTokenTest() {
        token.transfer(relay.contractAddress, BigInteger.valueOf(987_654)).send()
        assertEquals(BigInteger.valueOf(0), token.balanceOf(master.contractAddress).send())
        assertEquals(BigInteger.valueOf(987_654), token.balanceOf(relay.contractAddress).send())
        Assertions.assertThrows(TransactionException::class.java) { relay.sendToMaster(token.contractAddress).send() }
    }

    /**
     * @given deployed master contract
     * @when 4 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 4 signatures from added peers
     * @then withdraw call succeeded
     */
    @Test
    fun validSignatures4of4() {
        val sigCount = 4
        val amountToSend = 1000
        val initialBalance =
            deployHelper.web3.ethGetBalance(accGreen, DefaultBlockParameterName.LATEST).send().balance
        deployHelper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)

        val finalHash =
            hashToWithdraw(
                etherAddress,
                amountToSend.toString(),
                accGreen,
                defaultIrohaHash,
                relay.contractAddress
            )

        val keypairs = ArrayList<ECKeyPair>()
        for (i in 0 until sigCount) {
            val keypair = Keys.createEcKeyPair()
            keypairs.add(keypair)
            sendAddPeer("0x" + Keys.getAddress(keypair))
        }
        master.disableAddingNewPeers().send()
        val sigs = prepareSignatures(sigCount, keypairs, finalHash)

        addWhiteListToRelayRegistry(Keys.getAddress(keypair), listOf(accGreen))

        master.withdraw(
            etherAddress,
            BigInteger.valueOf(amountToSend.toLong()),
            accGreen,
            defaultByteHash,
            sigs.vv,
            sigs.rr,
            sigs.ss,
            relay.contractAddress
        ).send()

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
     * @given deployed master contract
     * @when 4 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 3 signatures from added peers
     * @then withdraw call succeeded
     */
    @Test
    fun validSignatures3of4() {
        val sigCount = 4
        val realSigCount = 3
        val amountToSend = 1000
        val tokenAddress = etherAddress
        val initialBalance =
            deployHelper.web3.ethGetBalance(accGreen, DefaultBlockParameterName.LATEST).send().balance
        deployHelper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)

        val finalHash =
            hashToWithdraw(
                tokenAddress,
                amountToSend.toString(),
                accGreen,
                defaultIrohaHash,
                relay.contractAddress
            )

        val keypairs = ArrayList<ECKeyPair>()
        for (i in 0 until sigCount) {
            val keypair = Keys.createEcKeyPair()
            keypairs.add(keypair)
            sendAddPeer("0x" + Keys.getAddress(keypair))
        }
        val sigs = prepareSignatures(realSigCount, keypairs.subList(0, realSigCount), finalHash)

        master.disableAddingNewPeers().send()
        addWhiteListToRelayRegistry(Keys.getAddress(keypair), listOf(accGreen))

        master.withdraw(
            tokenAddress,
            BigInteger.valueOf(amountToSend.toLong()),
            accGreen,
            defaultByteHash,
            sigs.vv,
            sigs.rr,
            sigs.ss,
            relay.contractAddress
        ).send()

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
     * @given deployed master contract
     * @when 5 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 3 signatures from added peers
     * @then withdraw call failed
     */
    @Test
    fun validSignatures3of5() {
        val sigCount = 5
        val realSigCount = 3
        val amountToSend = 1000
        val tokenAddress = etherAddress

        val finalHash =
            hashToWithdraw(
                tokenAddress,
                amountToSend.toString(),
                accGreen,
                defaultIrohaHash,
                relay.contractAddress
            )

        val keypairs = ArrayList<ECKeyPair>()
        for (i in 0 until sigCount) {
            val keypair = Keys.createEcKeyPair()
            keypairs.add(keypair)
            sendAddPeer("0x" + Keys.getAddress(keypair))
        }
        master.disableAddingNewPeers().send()
        val sigs = prepareSignatures(realSigCount, keypairs.subList(0, realSigCount), finalHash)

        Assertions.assertThrows(TransactionException::class.java) {
            master.withdraw(
                tokenAddress,
                BigInteger.valueOf(amountToSend.toLong()),
                accGreen,
                defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                relay.contractAddress
            ).send()
        }
    }

    /**
     * @given deployed master contract
     * @when 100 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 100 signatures from added peers
     * @then withdraw call succeeded
     */
    @Test
    fun validSignatures100of100() {
        val sigCount = 100
        val amountToSend = 1000
        val tokenAddress = etherAddress
        val initialBalance =
            deployHelper.web3.ethGetBalance(accGreen, DefaultBlockParameterName.LATEST).send().balance
        deployHelper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)

        val finalHash =
            hashToWithdraw(
                tokenAddress,
                amountToSend.toString(),
                accGreen,
                defaultIrohaHash,
                relay.contractAddress
            )

        val keypairs = ArrayList<ECKeyPair>()
        for (i in 0 until sigCount) {
            val keypair = Keys.createEcKeyPair()
            keypairs.add(keypair)
            sendAddPeer("0x" + Keys.getAddress(keypair))
        }
        master.disableAddingNewPeers().send()
        val sigs = prepareSignatures(sigCount, keypairs, finalHash)

        addWhiteListToRelayRegistry(Keys.getAddress(keypair), listOf(accGreen))

        master.withdraw(
            tokenAddress,
            BigInteger.valueOf(amountToSend.toLong()),
            accGreen,
            defaultByteHash,
            sigs.vv,
            sigs.rr,
            sigs.ss,
            relay.contractAddress
        ).send()

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
     * @given deployed master contract
     * @when 100 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 67 signatures from added peers
     * @then withdraw call succeeded
     */
    @Test
    fun validSignatures67of100() {
        val sigCount = 100
        val realSigCount = 67
        val amountToSend = 1000
        val tokenAddress = etherAddress
        val initialBalance =
            deployHelper.web3.ethGetBalance(accGreen, DefaultBlockParameterName.LATEST).send().balance
        deployHelper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)

        val finalHash =
            hashToWithdraw(
                tokenAddress,
                amountToSend.toString(),
                accGreen,
                defaultIrohaHash,
                relay.contractAddress
            )

        val keypairs = ArrayList<ECKeyPair>()
        for (i in 0 until sigCount) {
            val keypair = Keys.createEcKeyPair()
            keypairs.add(keypair)
            sendAddPeer("0x" + Keys.getAddress(keypair))
        }
        master.disableAddingNewPeers().send()
        val sigs = prepareSignatures(realSigCount, keypairs.subList(0, realSigCount), finalHash)

        addWhiteListToRelayRegistry(Keys.getAddress(keypair), listOf(accGreen))

        master.withdraw(
            tokenAddress,
            BigInteger.valueOf(amountToSend.toLong()),
            accGreen,
            defaultByteHash,
            sigs.vv,
            sigs.rr,
            sigs.ss,
            relay.contractAddress
        ).send()

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
     * @given deployed master contract
     * @when 100 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 66 signatures from added peers
     * @then withdraw call failed
     */
    @Test
    fun validSignatures66of100() {
        val sigCount = 100
        val realSigCount = 66
        val amountToSend = 1000
        val tokenAddress = etherAddress

        val finalHash =
            hashToWithdraw(
                tokenAddress,
                amountToSend.toString(),
                accGreen,
                defaultIrohaHash,
                relay.contractAddress
            )

        val keypairs = ArrayList<ECKeyPair>()
        for (i in 0 until sigCount) {
            val keypair = Keys.createEcKeyPair()
            keypairs.add(keypair)
            sendAddPeer("0x" + Keys.getAddress(keypair))
        }
        master.disableAddingNewPeers().send()
        val sigs = prepareSignatures(realSigCount, keypairs.subList(0, realSigCount), finalHash)

        Assertions.assertThrows(TransactionException::class.java) {
            master.withdraw(
                tokenAddress,
                BigInteger.valueOf(amountToSend.toLong()),
                accGreen,
                defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                relay.contractAddress
            ).send()
        }
    }

    /**
     * @given deployed relay registry contract
     * @when add new relay twice
     * @then second call throws exception
     */
    @Test
    fun addSameWhiteListTwice() {
        addWhiteListToRelayRegistry(Keys.getAddress(keypair), listOf(accGreen)) // first call
        Assertions.assertThrows(TransactionException::class.java) {
            addWhiteListToRelayRegistry(
                Keys.getAddress(keypair),
                listOf(accGreen)
            )
        } // second call
    }

    /**
     * @given deployed relay registry contract
     * @when add new white list and check address in white list
     * @then address should be in the list
     */
    @Test
    fun addWhiteListAndCheckAddress() {
        addWhiteListToRelayRegistry(Keys.getAddress(keypair), listOf(accGreen))
        Assertions.assertTrue(relayRegistry.isWhiteListed(Keys.getAddress(keypair), accGreen).send())
    }

    /**
     * @given relay registry contract
     * @when add list of 100 addresses
     * @then all addresses should be stored successful
     */
    @Test
    fun addOneHundredAddressesInWhiteListAndCheckThem() {
        val whiteListSize = 100
        var equalAddressesSize = 0
        val whiteList = mutableListOf<String>()
        for (i in 0 until whiteListSize) {
            val keypair = Keys.createEcKeyPair()
            whiteList.add(Keys.getAddress(keypair))
        }

        val addressList: List<String> = whiteList.toList()

        addWhiteListToRelayRegistry(Keys.getAddress(keypair), addressList)

        for (i in 0 until addressList.size) {
            if (relayRegistry.isWhiteListed(
                    Keys.getAddress(keypair),
                    addressList[i]
                ).send()
            ) {
                equalAddressesSize++
            }
        }

        assertEquals(equalAddressesSize, relayRegistry.getWhiteListByRelay(Keys.getAddress(keypair)).send().size)
    }

    /**
     * @given relay registry contract
     * @when withdraw from not stored relay
     * @then should be success
     */
    @Test
    fun withdrawWithNotStoredRelay() {
        val sigCount = 4
        val amountToSend = 1000
        val initialBalance =
            deployHelper.web3.ethGetBalance(accGreen, DefaultBlockParameterName.LATEST).send().balance
        deployHelper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)

        val finalHash =
            hashToWithdraw(
                etherAddress,
                amountToSend.toString(),
                accGreen,
                defaultIrohaHash,
                relay.contractAddress
            )

        val keypairs = ArrayList<ECKeyPair>()
        for (i in 0 until sigCount) {
            val keypair = Keys.createEcKeyPair()
            keypairs.add(keypair)
            sendAddPeer("0x" + Keys.getAddress(keypair))
        }
        master.disableAddingNewPeers().send()
        val sigs = prepareSignatures(sigCount, keypairs, finalHash)

        master.withdraw(
            etherAddress,
            BigInteger.valueOf(amountToSend.toLong()),
            accGreen,
            defaultByteHash,
            sigs.vv,
            sigs.rr,
            sigs.ss,
            relay.contractAddress
        ).send()

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
     * @given relay registry and master contract
     * @when try to withdraw to address which is not in whitelist
     * @then should throws exception
     */
    @Test
    fun withdrawToAddressAbsentInWhiteList() {
        addWhiteListToRelayRegistry(Keys.getAddress(keypair), listOf(accGreen))
        Assertions.assertThrows(TransactionException::class.java) {
            withdraw(
                "0x1", //wrong address
                BigInteger.valueOf(4000)
            )
        }

    }
}
