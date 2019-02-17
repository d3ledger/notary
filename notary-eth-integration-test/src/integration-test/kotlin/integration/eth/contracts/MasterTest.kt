package integration.eth.contracts

import contract.BasicCoin
import contract.Master
import integration.helper.ContractTestHelper
import integration.helper.IrohaConfigHelper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.protocol.exceptions.TransactionException
import sidechain.eth.util.hashToAddAndRemovePeer
import sidechain.eth.util.hashToWithdraw
import java.math.BigInteger
import java.time.Duration

class MasterTest {
    private lateinit var cth: ContractTestHelper
    private lateinit var master: Master
    private lateinit var token: BasicCoin
    private lateinit var accMain: String
    private lateinit var accGreen: String
    private lateinit var keypair: ECKeyPair
    private lateinit var etherAddress: String

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

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
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val initialBalance = cth.getETHBalance(master.contractAddress)
            cth.sendEthereum(BigInteger.valueOf(300_000_000), master.contractAddress)
            Assertions.assertEquals(
                initialBalance + BigInteger.valueOf(300_000_000),
                cth.getETHBalance(master.contractAddress)
            )
        }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master
     * @then call to withdraw succeeded
     */
    @Test
    fun singleCorrectSignatureTokenTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            master.addToken(token.contractAddress).send()
            cth.transferTokensToMaster(BigInteger.valueOf(5))
            cth.withdraw(BigInteger.valueOf(1))
            Assertions.assertEquals(BigInteger.valueOf(4), token.balanceOf(master.contractAddress).send())
        }
    }

    /**
     * @given deployed master and token contracts
     * @when two peers added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master
     * @then call to withdraw fails
     */
    @Test
    fun singleNotEnoughSignaturesTokenTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            cth.transferTokensToMaster(BigInteger.valueOf(5))
            Assertions.assertThrows(TransactionException::class.java) { cth.withdraw(BigInteger.valueOf(1)) }
        }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5000 WEI transferred to master,
     * request to withdraw 10000 WEI is sent to master
     * @then call to withdraw fails
     */
    @Test
    fun notEnoughEtherTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
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
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 10 tokens is sent to master
     * @then call to withdraw fails
     */
    @Test
    fun notEnoughTokensTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            cth.transferTokensToMaster(BigInteger.valueOf(5))
            Assertions.assertThrows(TransactionException::class.java) { cth.withdraw(BigInteger.valueOf(10)) }
        }
    }

    /**
     * @given deployed master contract
     * @when one peer added to master, 5000 Wei transferred to master,
     * request to withdraw 1000 Wei is sent to master
     * @then call to withdraw succeeded
     */
    @Test
    fun singleCorrectSignatureEtherTestMaster() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val initialBalance = cth.getETHBalance(accGreen)
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
    }

    /**
     * @given deployed master contract
     * @when one peer added to master, 5000 Wei transferred to master,
     * request to withdraw 10000 Wei is sent to master - withdrawal fails
     * then add 5000 Wei to master (simulate vacuum process) and
     * request to withdraw 10000 Wei is sent to master
     * @then the second call to withdraw succeeded
     */
    @Test
    fun secondWithdrawAfterVacuum() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val initialBalance = cth.getETHBalance(accGreen)

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

            cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
            cth.withdraw(
                BigInteger.valueOf(10000),
                accGreen,
                etherAddress
            )
            Assertions.assertEquals(
                BigInteger.valueOf(0),
                cth.getETHBalance(master.contractAddress)
            )
            Assertions.assertEquals(
                initialBalance + BigInteger.valueOf(10000),
                cth.getETHBalance(accGreen)
            )
        }
    }

    /**
     * @given deployed master and token contracts
     * @when 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master
     * @then withdraw attempt fails
     */
    @Test
    fun noPeersWithdraw() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            cth.transferTokensToMaster(BigInteger.valueOf(5))
            Assertions.assertThrows(TransactionException::class.java) { cth.withdraw(BigInteger.valueOf(1)) }
        }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master with r-array larger than v and s
     * @then withdraw attempt fails
     */
    @Test
    fun differentVRS() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
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
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
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
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
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
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master with invalid signature
     * @then call to withdraw fails
     */
    @Test
    fun invalidSignature() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
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
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master twice
     * @then second call to withdraw failed
     */
    @Test
    fun usedHashTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            master.addToken(token.contractAddress).send()
            cth.transferTokensToMaster(BigInteger.valueOf(5))
            cth.withdraw(BigInteger.valueOf(1))
            Assertions.assertEquals(BigInteger.valueOf(4), token.balanceOf(master.contractAddress).send())
            Assertions.assertThrows(TransactionException::class.java) { cth.withdraw(BigInteger.valueOf(1)) }
        }
    }

    /**
     * @given deployed master contract
     * @when addToken called twice with different addresses
     * @then both calls succeeded, both tokens are added
     */
    @Test
    fun addTokenTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val fakeTokenAddress = "0xf230b790e05390fc8295f4d3f60332c93bed42d1"
            master.addToken(token.contractAddress).send()
            master.addToken(fakeTokenAddress).send()
            val res = master.tokens.send()
            Assertions.assertEquals(2, res.size)
            Assertions.assertEquals(token.contractAddress, res[0])
            Assertions.assertEquals(fakeTokenAddress, res[1])
        }
    }

    /**
     * @given deployed master contract
     * @when addToken called twice with same addresses
     * @then second call throws exception
     */
    @Test
    fun addSameTokensTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            master.addToken(token.contractAddress).send()
            Assertions.assertThrows(TransactionException::class.java) { master.addToken(token.contractAddress).send() }
        }
    }

    /**
     * @given deployed master contract
     * @when 4 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 4 signatures from added peers
     * @then withdraw call succeeded
     */
    @Test
    fun validSignatures4of4() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 4
            val amountToSend = 1000
            val initialBalance = cth.getETHBalance(accGreen)
            val keypairs = ArrayList<ECKeyPair>()
            val peers = ArrayList<String>()
            for (i in 0 until sigCount) {
                val keypair = Keys.createEcKeyPair()
                keypairs.add(keypair)
                peers.add("0x" + Keys.getAddress(keypair))
            }

            val master = cth.deployMaster(
                cth.relayRegistry.contractAddress,
                peers
            )

            cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)

            val finalHash =
                hashToWithdraw(
                    etherAddress,
                    amountToSend.toString(),
                    accGreen,
                    cth.defaultIrohaHash,
                    master.contractAddress
                )

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
    }

    /**
     * @given deployed master contract
     * @when 4 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 3 signatures from added peers
     * @then withdraw call succeeded
     */
    @Test
    fun validSignatures3of4() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 4
            val realSigCount = 3
            val amountToSend = 1000
            val tokenAddress = etherAddress
            val initialBalance = cth.getETHBalance(accGreen)
            val keypairs = ArrayList<ECKeyPair>()
            val peers = ArrayList<String>()
            for (i in 0 until sigCount) {
                val keypair = Keys.createEcKeyPair()
                keypairs.add(keypair)
                peers.add("0x" + Keys.getAddress(keypair))
            }

            val master = cth.deployMaster(
                cth.relayRegistry.contractAddress,
                peers
            )

            cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)

            val finalHash =
                hashToWithdraw(
                    tokenAddress,
                    amountToSend.toString(),
                    accGreen,
                    cth.defaultIrohaHash,
                    master.contractAddress
                )

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
    }

    /**
     * @given deployed master contract
     * @when 5 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 3 signatures from added peers
     * @then withdraw call failed
     */
    @Test
    fun validSignatures3of5() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 5
            val realSigCount = 3
            val amountToSend = 1000
            val tokenAddress = etherAddress

            val keypairs = ArrayList<ECKeyPair>()
            val peers = ArrayList<String>()
            for (i in 0 until sigCount) {
                val keypair = Keys.createEcKeyPair()
                keypairs.add(keypair)
                peers.add("0x" + Keys.getAddress(keypair))
            }

            val master = cth.deployMaster(
                cth.relayRegistry.contractAddress,
                peers
            )

            val finalHash =
                hashToWithdraw(
                    tokenAddress,
                    amountToSend.toString(),
                    accGreen,
                    cth.defaultIrohaHash,
                    master.contractAddress
                )

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
    }

    /**
     * @given deployed master contract
     * @when 100 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 100 signatures from added peers
     * @then withdraw call succeeded
     */
    @Test
    fun validSignatures100of100() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 100
            val amountToSend = 1000
            val tokenAddress = etherAddress
            val initialBalance = cth.getETHBalance(accGreen)
            val keypairs = ArrayList<ECKeyPair>()
            val peers = ArrayList<String>()
            for (i in 0 until sigCount) {
                val keypair = Keys.createEcKeyPair()
                keypairs.add(keypair)
                peers.add("0x" + Keys.getAddress(keypair))
            }

            val master = cth.deployMaster(
                cth.relayRegistry.contractAddress,
                peers
            )

            cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)

            val finalHash =
                hashToWithdraw(
                    tokenAddress,
                    amountToSend.toString(),
                    accGreen,
                    cth.defaultIrohaHash,
                    master.contractAddress
                )

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
    }

    /**
     * @given deployed master contract
     * @when 100 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 67 signatures from added peers
     * @then withdraw call succeeded
     */
    @Test
    fun validSignatures67of100() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 100
            val realSigCount = 67
            val amountToSend = 1000
            val tokenAddress = etherAddress
            val initialBalance = cth.getETHBalance(accGreen)
            val keypairs = ArrayList<ECKeyPair>()
            val peers = ArrayList<String>()
            for (i in 0 until sigCount) {
                val keypair = Keys.createEcKeyPair()
                keypairs.add(keypair)
                peers.add("0x" + Keys.getAddress(keypair))
            }

            val master = cth.deployMaster(
                cth.relayRegistry.contractAddress,
                peers
            )
            cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)

            val finalHash =
                hashToWithdraw(
                    tokenAddress,
                    amountToSend.toString(),
                    accGreen,
                    cth.defaultIrohaHash,
                    master.contractAddress
                )

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
    }

    /**
     * @given deployed master contract
     * @when 100 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 66 signatures from added peers
     * @then withdraw call failed
     */
    @Test
    fun validSignatures66of100() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 100
            val realSigCount = 66
            val amountToSend = 1000
            val tokenAddress = etherAddress

            val keypairs = ArrayList<ECKeyPair>()
            val peers = ArrayList<String>()
            for (i in 0 until sigCount) {
                val keypair = Keys.createEcKeyPair()
                keypairs.add(keypair)
                peers.add("0x" + Keys.getAddress(keypair))
            }

            val master = cth.deployMaster(
                cth.relayRegistry.contractAddress,
                peers
            )

            val finalHash =
                hashToWithdraw(
                    tokenAddress,
                    amountToSend.toString(),
                    accGreen,
                    cth.defaultIrohaHash,
                    master.contractAddress
                )

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
    }

    /**
     * @given relay registry and master contract
     * @when try to withdraw to address which is not in whitelist
     * @then should throws exception
     */
    @Test
    fun withdrawToAddressAbsentInWhiteList() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            cth.addWhiteListToRelayRegistry(Keys.getAddress(keypair), listOf(accGreen))
            Assertions.assertThrows(TransactionException::class.java) {
                cth.withdraw(BigInteger.valueOf(4000), "0x1")   //wrong address
            }
        }
    }

    /**
     * @given relay registry and master contracts
     * @when try to add one more peer address by all valid peers
     * @then the new peer should be added
     */
    @Test
    fun addNewPeerByPeers4of4() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val newPeer = "0x006fe444ffbaffb27813265c50a479897b8a2514"
            val sigCount = 4
            val realSigCount = 4

            val finalHash =
                hashToAddAndRemovePeer(
                    newPeer,
                    cth.defaultIrohaHash
                )

            val keypairs = ArrayList<ECKeyPair>()
            val peers = ArrayList<String>()

            for (i in 0 until sigCount) {
                val keypair = Keys.createEcKeyPair()
                keypairs.add(keypair)
                peers.add(Keys.getAddress(keypair))
            }
            val master = cth.deployMaster(
                cth.relayRegistry.contractAddress,
                peers
            )

            val sigs = cth.prepareSignatures(realSigCount, keypairs.subList(0, realSigCount), finalHash)

            val result = master.addPeerByPeer(
                newPeer,
                cth.defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss
            ).send().isStatusOK

            Assertions.assertTrue(result)
            Assertions.assertTrue(master.peers(newPeer).send())
        }
    }

    /**
     * @given relay registry and master contracts
     * @when try to add one more peer address by all valid peers
     * @then the tx should be failed and new peer is not saved
     */
    @Test
    fun addNewPeerByPeers2of4() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val newPeer = "0x006fe444ffbaffb27813265c50a479897b8a2514"
            val sigCount = 4
            val realSigCount = 2

            val finalHash =
                hashToAddAndRemovePeer(
                    newPeer,
                    cth.defaultIrohaHash
                )

            val keypairs = ArrayList<ECKeyPair>()
            val peers = ArrayList<String>()

            for (i in 0 until sigCount) {
                val keypair = Keys.createEcKeyPair()
                keypairs.add(keypair)
                peers.add(Keys.getAddress(keypair))
            }
            val master = cth.deployMaster(
                cth.relayRegistry.contractAddress,
                peers
            )

            val sigs = cth.prepareSignatures(realSigCount, keypairs.subList(0, realSigCount), finalHash)

            Assertions.assertThrows(TransactionException::class.java) {
                master.addPeerByPeer(
                    newPeer,
                    cth.defaultByteHash,
                    sigs.vv,
                    sigs.rr,
                    sigs.ss
                ).send().isStatusOK
            }

            Assertions.assertFalse(master.peers(newPeer).send())
        }
    }

    /**
     * @given relay registry and master contracts
     * @when try to remove one peer address by rest of peers
     * @then the tx should be successful and peer is removed
     */
    @Test
    fun removePeerByPeers() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val peerToRemove = "0x006fe444ffbaffb27813265c50a479897b8a2514"
            val sigCount = 4
            val realSigCount = 4

            val keypairs = ArrayList<ECKeyPair>()
            val peers = ArrayList<String>()

            for (i in 0 until sigCount) {
                val keypair = Keys.createEcKeyPair()
                keypairs.add(keypair)
                peers.add(Keys.getAddress(keypair))
            }

            // deploy with peer which will be removed
            peers.add(peerToRemove)
            val master = cth.deployMaster(
                cth.relayRegistry.contractAddress,
                peers
            )

            val finalHash =
                hashToAddAndRemovePeer(
                    peers.first(),
                    cth.defaultIrohaHash
                )

            val sigs = cth.prepareSignatures(realSigCount, keypairs.subList(0, realSigCount), finalHash)

            Assertions.assertTrue(master.peers(peerToRemove).send())
            Assertions.assertTrue(
                master.removePeerByPeer(
                    peers.first(),
                    cth.defaultByteHash,
                    sigs.vv,
                    sigs.rr,
                    sigs.ss
                ).send().isStatusOK
            )

            Assertions.assertFalse(master.peers(peers.first()).send())
        }
    }
}
