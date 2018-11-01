package integration.eth.contracts

import contract.BasicCoin
import contract.Master
import integration.helper.ContractTestHelper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.protocol.exceptions.TransactionException
import sidechain.eth.util.hashToWithdraw
import java.math.BigInteger

class MasterTest {
    private lateinit var cth: ContractTestHelper
    private lateinit var master: Master
    private lateinit var token: BasicCoin
    private lateinit var accMain: String
    private lateinit var accGreen: String
    private lateinit var keypair: ECKeyPair
    private lateinit var etherAddress: String

    @BeforeEach
    fun setup() {
        cth = ContractTestHelper()
        master = cth.master
        token = cth.token
        accMain = cth.accMain
        accGreen = cth.accGreen
        keypair = cth.keypair
        etherAddress = cth.etherAddress
    }

    /**
     * @given master account deployed
     * @when transfer 300_000_000 WEIs to master account
     * @then balance of master account increased by 300_000_000
     */
    @Test
    fun canAcceptEther() {
        val initialBalance = cth.getETHBalance(master.contractAddress)
        cth.sendEthereum(BigInteger.valueOf(300_000_000), master.contractAddress)
        Assertions.assertEquals(
            initialBalance + BigInteger.valueOf(300_000_000),
            cth.getETHBalance(master.contractAddress)
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
        cth.sendAddPeer(accMain)
        master.disableAddingNewPeers().send()
        master.addToken(token.contractAddress).send()
        cth.transferTokensToMaster(BigInteger.valueOf(5))
        cth.withdraw(BigInteger.valueOf(1))
        Assertions.assertEquals(BigInteger.valueOf(4), token.balanceOf(master.contractAddress).send())
    }

    /**
     * @given deployed master and token contracts
     * @when two peers added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master
     * @then call to withdraw fails
     */
    @Test
    fun singleNotEnoughSignaturesTokenTest() {
        cth.sendAddPeer(accMain)
        cth.sendAddPeer(accGreen)
        master.disableAddingNewPeers().send()
        cth.transferTokensToMaster(BigInteger.valueOf(5))
        Assertions.assertThrows(TransactionException::class.java) { cth.withdraw(BigInteger.valueOf(1)) }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5000 WEI transferred to master,
     * request to withdraw 10000 WEI is sent to master
     * @then call to withdraw fails
     */
    @Test
    fun notEnoughEtherTest() {
        cth.sendAddPeer(accMain)
        master.disableAddingNewPeers().send()
        cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
        val call =
            cth.withdraw(
                BigInteger.valueOf(10000),
                accGreen,
                etherAddress
            )
        Assertions.assertEquals(
            "0x" + "0".repeat(24) +
                    etherAddress.slice(2 until etherAddress.length),
            call.logs[0].data.subSequence(0, 66)
        )
        Assertions.assertEquals(accGreen, "0x" + call.logs[0].data.subSequence(90, 130))
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 10 tokens is sent to master
     * @then call to withdraw fails
     */
    @Test
    fun notEnoughTokensTest() {
        cth.sendAddPeer(accMain)
        master.disableAddingNewPeers().send()
        cth.transferTokensToMaster(BigInteger.valueOf(5))
        Assertions.assertThrows(TransactionException::class.java) { cth.withdraw(BigInteger.valueOf(10)) }
    }

    /**
     * @given deployed master contract
     * @when one peer added to master, 5000 Wei transferred to master,
     * request to withdraw 1000 Wei is sent to master
     * @then call to withdraw succeeded
     */
    @Test
    fun singleCorrectSignatureEtherTestMaster() {
        val initialBalance = cth.getETHBalance(accGreen)
        cth.sendAddPeer(accMain)
        master.disableAddingNewPeers().send()
        cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
        cth.withdraw(
            BigInteger.valueOf(1000),
            accGreen,
            etherAddress
        )
        Assertions.assertEquals(
            BigInteger.valueOf(4000),
            cth.getETHBalance(master.contractAddress)
        )
        Assertions.assertEquals(
            initialBalance + BigInteger.valueOf(1000),
            cth.getETHBalance(accGreen)
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
        cth.transferTokensToMaster(BigInteger.valueOf(5))
        Assertions.assertThrows(TransactionException::class.java) { cth.withdraw(BigInteger.valueOf(1)) }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master with r-array larger than v and s
     * @then withdraw attempt fails
     */
    @Test
    fun differentVRS() {
        cth.sendAddPeer(accMain)
        master.disableAddingNewPeers().send()
        cth.transferTokensToMaster(BigInteger.valueOf(5))

        val amount = BigInteger.valueOf(1)
        val finalHash =
            hashToWithdraw(
                token.contractAddress,
                amount.toString(),
                accGreen,
                cth.defaultIrohaHash,
                master.contractAddress
            )

        val sigs = cth.prepareSignatures(1, listOf(keypair), finalHash)
        sigs.rr.add(sigs.rr.first())

        Assertions.assertThrows(TransactionException::class.java) {
            master.withdraw(
                token.contractAddress,
                amount,
                accGreen,
                cth.defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                master.contractAddress
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
        cth.sendAddPeer(accMain)
        master.disableAddingNewPeers().send()
        cth.transferTokensToMaster(BigInteger.valueOf(5))

        val amount = BigInteger.valueOf(1)
        val finalHash =
            hashToWithdraw(
                token.contractAddress,
                amount.toString(),
                accGreen,
                cth.defaultIrohaHash,
                master.contractAddress
            )
        val sigs = cth.prepareSignatures(2, listOf(keypair, keypair), finalHash)

        Assertions.assertThrows(TransactionException::class.java) {
            master.withdraw(
                token.contractAddress,
                amount,
                accGreen,
                cth.defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                master.contractAddress
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
        cth.sendAddPeer(accMain)
        master.disableAddingNewPeers().send()
        cth.transferTokensToMaster(BigInteger.valueOf(5))

        val amount = BigInteger.valueOf(1)
        val finalHash =
            hashToWithdraw(
                token.contractAddress,
                amount.toString(),
                accGreen,
                cth.defaultIrohaHash,
                master.contractAddress
            )
        val sigs = cth.prepareSignatures(2, listOf(keypair, Keys.createEcKeyPair()), finalHash)

        Assertions.assertThrows(TransactionException::class.java) {
            master.withdraw(
                token.contractAddress,
                amount,
                accGreen,
                cth.defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                master.contractAddress
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
        cth.sendAddPeer(accMain)
        master.disableAddingNewPeers().send()
        cth.transferTokensToMaster(BigInteger.valueOf(5))

        val amount = BigInteger.valueOf(1)
        val finalHash =
            hashToWithdraw(
                token.contractAddress,
                amount.toString(),
                accGreen,
                cth.defaultIrohaHash,
                master.contractAddress
            )

        val sigs = cth.prepareSignatures(1, listOf(keypair), finalHash)
        sigs.ss.first()[0] = sigs.ss.first()[0].inc()

        Assertions.assertThrows(TransactionException::class.java) {
            master.withdraw(
                token.contractAddress,
                amount,
                accGreen,
                cth.defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                master.contractAddress
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
        cth.sendAddPeer(accMain)
        master.disableAddingNewPeers().send()
        master.addToken(token.contractAddress).send()
        cth.transferTokensToMaster(BigInteger.valueOf(5))
        cth.withdraw(BigInteger.valueOf(1))
        Assertions.assertEquals(BigInteger.valueOf(4), token.balanceOf(master.contractAddress).send())
        Assertions.assertThrows(TransactionException::class.java) { cth.withdraw(BigInteger.valueOf(1)) }
    }

    /**
     * @given deployed master contract
     * @when AddPeer called twice with different addresses
     * @then both calls succeeded
     */
    @Test
    fun addPeerTest() {
        cth.sendAddPeer(accMain)
        cth.sendAddPeer(accGreen)
    }

    /**
     * @given deployed master contract
     * @when AddPeer called twice with same addresses
     * @then second call fails
     */
    @Test
    fun addSamePeer() {
        cth.sendAddPeer(accGreen)
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
        Assertions.assertEquals(2, res.size)
        Assertions.assertEquals(token.contractAddress, res[0])
        Assertions.assertEquals(fakeTokenAddress, res[1])
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
     * @given deployed master contract
     * @when 4 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 4 signatures from added peers
     * @then withdraw call succeeded
     */
    @Test
    fun validSignatures4of4() {
        val sigCount = 4
        val amountToSend = 1000
        val initialBalance = cth.getETHBalance(accGreen)
        cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)

        val finalHash =
            hashToWithdraw(
                etherAddress,
                amountToSend.toString(),
                accGreen,
                cth.defaultIrohaHash,
                master.contractAddress
            )

        val keypairs = ArrayList<ECKeyPair>()
        for (i in 0 until sigCount) {
            val keypair = Keys.createEcKeyPair()
            keypairs.add(keypair)
            cth.sendAddPeer("0x" + Keys.getAddress(keypair))
        }
        master.disableAddingNewPeers().send()
        val sigs = cth.prepareSignatures(sigCount, keypairs, finalHash)

        master.withdraw(
            etherAddress,
            BigInteger.valueOf(amountToSend.toLong()),
            accGreen,
            cth.defaultByteHash,
            sigs.vv,
            sigs.rr,
            sigs.ss,
            master.contractAddress
        ).send()

        Assertions.assertEquals(BigInteger.valueOf(4000), cth.getETHBalance(master.contractAddress))
        Assertions.assertEquals(initialBalance + BigInteger.valueOf(1000), cth.getETHBalance(accGreen))
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
        val initialBalance = cth.getETHBalance(accGreen)
        cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)

        val finalHash =
            hashToWithdraw(
                tokenAddress,
                amountToSend.toString(),
                accGreen,
                cth.defaultIrohaHash,
                master.contractAddress
            )

        val keypairs = ArrayList<ECKeyPair>()
        for (i in 0 until sigCount) {
            val keypair = Keys.createEcKeyPair()
            keypairs.add(keypair)
            cth.sendAddPeer("0x" + Keys.getAddress(keypair))
        }
        master.disableAddingNewPeers().send()
        val sigs = cth.prepareSignatures(realSigCount, keypairs.subList(0, realSigCount), finalHash)

        master.withdraw(
            tokenAddress,
            BigInteger.valueOf(amountToSend.toLong()),
            accGreen,
            cth.defaultByteHash,
            sigs.vv,
            sigs.rr,
            sigs.ss,
            master.contractAddress
        ).send()

        Assertions.assertEquals(BigInteger.valueOf(4000), cth.getETHBalance(master.contractAddress))
        Assertions.assertEquals(initialBalance + BigInteger.valueOf(1000), cth.getETHBalance(accGreen))
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
                cth.defaultIrohaHash,
                master.contractAddress
            )

        val keypairs = ArrayList<ECKeyPair>()
        for (i in 0 until sigCount) {
            val keypair = Keys.createEcKeyPair()
            keypairs.add(keypair)
            cth.sendAddPeer("0x" + Keys.getAddress(keypair))
        }
        master.disableAddingNewPeers().send()
        val sigs = cth.prepareSignatures(realSigCount, keypairs.subList(0, realSigCount), finalHash)

        Assertions.assertThrows(TransactionException::class.java) {
            master.withdraw(
                tokenAddress,
                BigInteger.valueOf(amountToSend.toLong()),
                accGreen,
                cth.defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                master.contractAddress
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
        val initialBalance = cth.getETHBalance(accGreen)
        cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)

        val finalHash =
            hashToWithdraw(
                tokenAddress,
                amountToSend.toString(),
                accGreen,
                cth.defaultIrohaHash,
                master.contractAddress
            )

        val keypairs = ArrayList<ECKeyPair>()
        for (i in 0 until sigCount) {
            val keypair = Keys.createEcKeyPair()
            keypairs.add(keypair)
            cth.sendAddPeer("0x" + Keys.getAddress(keypair))
        }
        master.disableAddingNewPeers().send()
        val sigs = cth.prepareSignatures(sigCount, keypairs, finalHash)

        master.withdraw(
            tokenAddress,
            BigInteger.valueOf(amountToSend.toLong()),
            accGreen,
            cth.defaultByteHash,
            sigs.vv,
            sigs.rr,
            sigs.ss,
            master.contractAddress
        ).send()

        Assertions.assertEquals(BigInteger.valueOf(4000), cth.getETHBalance(master.contractAddress))
        Assertions.assertEquals(initialBalance + BigInteger.valueOf(1000), cth.getETHBalance(accGreen))
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
        val initialBalance = cth.getETHBalance(accGreen)
        cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)

        val finalHash =
            hashToWithdraw(
                tokenAddress,
                amountToSend.toString(),
                accGreen,
                cth.defaultIrohaHash,
                master.contractAddress
            )

        val keypairs = ArrayList<ECKeyPair>()
        for (i in 0 until sigCount) {
            val keypair = Keys.createEcKeyPair()
            keypairs.add(keypair)
            cth.sendAddPeer("0x" + Keys.getAddress(keypair))
        }
        master.disableAddingNewPeers().send()
        val sigs = cth.prepareSignatures(realSigCount, keypairs.subList(0, realSigCount), finalHash)

        master.withdraw(
            tokenAddress,
            BigInteger.valueOf(amountToSend.toLong()),
            accGreen,
            cth.defaultByteHash,
            sigs.vv,
            sigs.rr,
            sigs.ss,
            master.contractAddress
        ).send()

        Assertions.assertEquals(BigInteger.valueOf(4000), cth.getETHBalance(master.contractAddress))
        Assertions.assertEquals(initialBalance + BigInteger.valueOf(1000), cth.getETHBalance(accGreen))
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
                cth.defaultIrohaHash,
                master.contractAddress
            )

        val keypairs = ArrayList<ECKeyPair>()
        for (i in 0 until sigCount) {
            val keypair = Keys.createEcKeyPair()
            keypairs.add(keypair)
            cth.sendAddPeer("0x" + Keys.getAddress(keypair))
        }
        master.disableAddingNewPeers().send()
        val sigs = cth.prepareSignatures(realSigCount, keypairs.subList(0, realSigCount), finalHash)

        Assertions.assertThrows(TransactionException::class.java) {
            master.withdraw(
                tokenAddress,
                BigInteger.valueOf(amountToSend.toLong()),
                accGreen,
                cth.defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                master.contractAddress
            ).send()
        }
    }

    /**
     * @given relay registry and master contract
     * @when try to withdraw to address which is not in whitelist
     * @then should throws exception
     */
    @Test
    fun withdrawToAddressAbsentInWhiteList() {
        cth.addWhiteListToRelayRegistry(Keys.getAddress(keypair), listOf(accGreen))
        Assertions.assertThrows(TransactionException::class.java) {
            cth.withdraw(BigInteger.valueOf(4000), "0x1")   //wrong address
        }
    }

    /**
     * @given relay registry and master contract
     * @when adding all peers as a list
     * @then all peers should be stored in the contract
     */
    @Test
    fun addAllPeersAtOnce() {
        val peers = listOf("0x01", "0x02", "0x03")
        val result = master.addPeers(peers).send()
        val n = master.peersCount().send()

        Assertions.assertTrue(result.isStatusOK)
        Assertions.assertEquals(3, n)
    }
}
