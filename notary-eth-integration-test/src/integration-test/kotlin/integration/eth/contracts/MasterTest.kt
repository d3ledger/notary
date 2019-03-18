package integration.eth.contracts

import com.d3.eth.sidechain.util.hashToAddAndRemovePeer
import com.d3.eth.sidechain.util.hashToMint
import com.d3.eth.sidechain.util.hashToWithdraw
import contract.BasicCoin
import contract.Master
import integration.helper.ContractTestHelper
import integration.helper.IrohaConfigHelper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.protocol.exceptions.TransactionException
import java.math.BigInteger
import java.time.Duration
import kotlin.test.assertEquals

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
     * Try to reinitialize master with another relay registry.
     * @given master contract is deployed and initialized
     * @when call initialize
     * @then call failed
     */
    @Test
    fun initializeAgain() {
        Assertions.assertThrows(TransactionException::class.java) {
            master.initialize(
                cth.testEthHelper.credentials.address,
                etherAddress,
                listOf("0x0000000000000000000000000000000000000000")
            ).send()
        }
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
     * @then both calls succeeded, both tokens are added, there are 3 tokens total (2 added + 1 XOR)
     */
    @Test
    fun addTokenTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val fakeTokenAddress = "0xf230b790e05390fc8295f4d3f60332c93bed42d1"
            master.addToken(token.contractAddress).send()
            master.addToken(fakeTokenAddress).send()
            val res = master.tokens.send()
            Assertions.assertEquals(3, res.size)
            Assertions.assertEquals(token.contractAddress, res[1])
            Assertions.assertEquals(fakeTokenAddress, res[2])
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
            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.testEthHelper.deployMasterSmartContract(
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

            val sigs = cth.prepareSignatures(sigCount, keyPairs, finalHash)

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
            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.testEthHelper.deployMasterSmartContract(
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

            val sigs = cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

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

            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.testEthHelper.deployMasterSmartContract(
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

            val sigs = cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

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
     * request to withdraw 1000 Wei is sent to master with 50 signatures from added peers
     * @then withdraw call succeeded
     */
    @Test
    fun validSignatures50of50() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 50
            val amountToSend = 1000
            val tokenAddress = etherAddress
            val initialBalance = cth.getETHBalance(accGreen)
            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.testEthHelper.deployMasterSmartContract(
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

            val sigs = cth.prepareSignatures(sigCount, keyPairs, finalHash)

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
     * @when 50 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 67 signatures from added peers
     * @then withdraw call succeeded
     */
    @Test
    fun validSignatures34of50() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 50
            val realSigCount = 34
            val amountToSend = 1000
            val tokenAddress = etherAddress
            val initialBalance = cth.getETHBalance(accGreen)
            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.testEthHelper.deployMasterSmartContract(
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

            val sigs = cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

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
     * @when 50 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 66 signatures from added peers
     * @then withdraw call failed
     */
    @Test
    fun validSignatures33of50() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 50
            val realSigCount = 33
            val amountToSend = 1000
            val tokenAddress = etherAddress

            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.testEthHelper.deployMasterSmartContract(
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

            val sigs = cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

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

            val finalHash = hashToAddAndRemovePeer(
                newPeer,
                cth.defaultIrohaHash
            )

            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.testEthHelper.deployMasterSmartContract(
                cth.relayRegistry.contractAddress,
                peers
            )

            val sigs = cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

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
     * @given relay registry and master contracts, peer is added with hash
     * @when try to remove peer address by all valid peers and then add peer by used hash
     * @then the peer should not be added again if tx hash is already used
     */
    @Test
    fun addNewPeerByPeersUseSameHash() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val newPeer = "0x006fe444ffbaffb27813265c50a479897b8a2512"
            val sigCount = 4
            val realSigCount = 4

            val finalHash = hashToAddAndRemovePeer(
                newPeer,
                cth.defaultIrohaHash
            )

            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.testEthHelper.deployMasterSmartContract(
                cth.relayRegistry.contractAddress,
                peers
            )

            val sigs = cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

            // first call
            val resultAdd = master.addPeerByPeer(
                newPeer,
                cth.defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss
            ).send().isStatusOK

            Assertions.assertTrue(resultAdd)
            Assertions.assertTrue(master.peers(newPeer).send())

            val removeIrohaHash = Hash.sha3(String.format("%064x", BigInteger.valueOf(54321)))
            val removeHash = hashToAddAndRemovePeer(
                newPeer,
                removeIrohaHash
            )
            val removeByteHash = cth.itohaHashToByteHash(removeIrohaHash)
            val removeSigs = cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), removeHash)

            // remove peer
            val resultRemove = master.removePeerByPeer(
                newPeer,
                removeByteHash,
                removeSigs.vv,
                removeSigs.rr,
                removeSigs.ss
            ).send().isStatusOK

            Assertions.assertTrue(resultRemove)
            Assertions.assertFalse(master.peers(newPeer).send())

            // second call to add peer with the same hash
            Assertions.assertThrows(TransactionException::class.java) {
                master.addPeerByPeer(
                    newPeer,
                    cth.defaultByteHash,
                    sigs.vv,
                    sigs.rr,
                    sigs.ss
                ).send()
            }

            Assertions.assertFalse(master.peers(newPeer).send())
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

            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)
            val master = cth.testEthHelper.deployMasterSmartContract(
                cth.relayRegistry.contractAddress,
                peers
            )

            val sigs = cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

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

            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            // deploy with peer which will be removed

            val withPeerToRemove = peers.toMutableList()
            withPeerToRemove.add(peerToRemove)
            val master = cth.testEthHelper.deployMasterSmartContract(
                cth.relayRegistry.contractAddress,
                withPeerToRemove
            )

            val finalHash =
                hashToAddAndRemovePeer(
                    withPeerToRemove.last(),
                    cth.defaultIrohaHash
                )

            val sigs = cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

            Assertions.assertTrue(master.peers(peerToRemove).send())
            Assertions.assertTrue(
                master.removePeerByPeer(
                    withPeerToRemove.last(),
                    cth.defaultByteHash,
                    sigs.vv,
                    sigs.rr,
                    sigs.ss
                ).send().isStatusOK
            )

            Assertions.assertFalse(master.peers(withPeerToRemove.last()).send())
        }
    }

    /**
     * @given master and sora token
     * @when try to mint
     * @then should be minted new tokens
     */
    @Test
    fun mintNewTokens4of4peers() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 4
            val realSigCount = 4

            val beneficiary = "0xbcBCeb4D66065B7b34d1B90f4fa572829F2c6D5c"
            val amountToSend = 1000

            val finalHash =
                hashToMint(
                    beneficiary,
                    amountToSend.toString(),
                    cth.defaultIrohaHash
                )

            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.testEthHelper.deployUpgradableMasterSmartContract(
                cth.relayRegistry.contractAddress,
                peers
            )
            val sigs = cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

            Assertions.assertTrue(
                master.mintTokensByPeers(
                    beneficiary,
                    BigInteger.valueOf(amountToSend.toLong()),
                    cth.defaultByteHash,
                    sigs.vv,
                    sigs.rr,
                    sigs.ss
                ).send().isStatusOK
            )

            Assertions.assertEquals(
                amountToSend,
                cth.getToken(master.xorTokenInstance().send()).balanceOf(beneficiary).send().toInt()
            )
        }
    }

    /**
     * @given master contract and sora token and mint is called with Iroha hash
     * @when mint is called with the same hash
     * @then mint call fails
     */
    @Test
    fun mintWithSameHash() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val beneficiary = "0xbcBCeb4D66065B7b34d1B90f4fa572829F2c6D5c"
            val amountToSend: Long = 1000

            val result = cth.mintByPeer(beneficiary, amountToSend).isStatusOK

            Assertions.assertTrue(result)
            Assertions.assertEquals(
                amountToSend,
                cth.getToken(master.xorTokenInstance().send()).balanceOf(beneficiary).send().toLong()
            )

            Assertions.assertThrows(TransactionException::class.java) {
                cth.mintByPeer(beneficiary, amountToSend)
            }
            Assertions.assertEquals(
                amountToSend,
                cth.getToken(master.xorTokenInstance().send()).balanceOf(beneficiary).send().toLong()
            )
        }
    }

    /**
     * @given master contract with token
     * @when check token balance
     * @then master contract balance should be equals to totalSupply
     */
    @Test
    fun checkMasterBalance() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            assertEquals(1, master.tokens.send().size)
            assertEquals(
                BigInteger("1618033988749894848204586834"),
                cth.getToken(master.xorTokenInstance().send()).balanceOf(master.contractAddress).send()
            )
        }
    }

    /**
     * @given master and sora token
     * @when try to mint
     * @then should not be minted new tokens
     */
    @Test
    fun mintNewTokens2of4peers() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 4
            val realSigCount = 2

            val beneficiary = "0xbcBCeb4D66065B7b34d1B90f4fa572829F2c6D5c"
            val amountToSend = 1000

            val finalHash =
                hashToMint(
                    beneficiary,
                    amountToSend.toString(),
                    cth.defaultIrohaHash
                )

            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.testEthHelper.deployUpgradableMasterSmartContract(
                cth.relayRegistry.contractAddress,
                peers
            )
            val sigs = cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

            Assertions.assertThrows(TransactionException::class.java) {
                master.mintTokensByPeers(
                    beneficiary,
                    BigInteger.valueOf(amountToSend.toLong()),
                    cth.defaultByteHash,
                    sigs.vv,
                    sigs.rr,
                    sigs.ss
                ).send()
            }
        }
    }
}
