package integration.eth.contracts

import contract.BasicCoin
import contract.Master
import contract.Relay
import integration.helper.ConfigHelper
import integration.helper.ContractTestHelper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.protocol.exceptions.TransactionException
import java.math.BigInteger
import java.time.Duration

class RelayTest {
    private lateinit var cth: ContractTestHelper
    private lateinit var master: Master
    private lateinit var token: BasicCoin
    private lateinit var relay: Relay
    private lateinit var accMain: String
    private lateinit var accGreen: String
    private lateinit var etherAddress: String

    private val timeoutDuration = Duration.ofMinutes(ConfigHelper.timeoutMinutes)

    @BeforeEach
    fun setup() {
        cth = ContractTestHelper()
        master = cth.master
        token = cth.token
        relay = cth.relay
        accMain = cth.accMain
        accGreen = cth.accGreen
        etherAddress = cth.etherAddress
    }

    /**
     * @given deployed master and relay contracts
     * @when one peer added to master, 5000 Wei transferred to master,
     * request to withdraw 1000 Wei is sent to relay
     * @then call to withdraw succeeded
     */
    @Test
    fun singleCorrectSignatureEtherTestRelay() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val initialBalance = cth.getETHBalance(accGreen)
            cth.sendAddPeer(accMain)
            master.disableAddingNewPeers().send()
            cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
            cth.withdraw(
                BigInteger.valueOf(1000),
                etherAddress,
                accGreen,
                false
            )
            Assertions.assertEquals(BigInteger.valueOf(4000), cth.getETHBalance(master.contractAddress))
            Assertions.assertEquals(initialBalance + BigInteger.valueOf(1000), cth.getETHBalance(accGreen))
        }
    }

    /**
     * @given deployed master and relay contracts
     * @when one peer added to master, 5000 Wei transferred to master,
     * request to withdraw 1000 Wei is sent to relay, destination address is also set to relay
     * @then call to withdraw succeeded
     */
    @Test
    fun singleCorrectSignatureEtherTestRelayToRelay() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val initialBalance = cth.getETHBalance(relay.contractAddress)
            cth.sendAddPeer(accMain)
            master.disableAddingNewPeers().send()
            cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
            cth.withdraw(
                BigInteger.valueOf(1000),
                etherAddress,
                relay.contractAddress,
                false
            )
            Assertions.assertEquals(BigInteger.valueOf(4000), cth.getETHBalance(master.contractAddress))
            Assertions.assertEquals(
                initialBalance + BigInteger.valueOf(1000),
                cth.getETHBalance(relay.contractAddress)
            )
        }
    }

    /**
     * @given deployed master and relay contracts, 100000 Wei is sent to relay
     * @when sendToMaster of relay contract for Ether is called
     * @then relay contract has 0 Ether, master contract has 100000 Wei
     */
    @Test
    fun vacuumEtherTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            cth.sendEthereum(BigInteger.valueOf(100_000), relay.contractAddress)
            Assertions.assertEquals(BigInteger.valueOf(0), cth.getETHBalance(master.contractAddress))
            Assertions.assertEquals(BigInteger.valueOf(100_000), cth.getETHBalance(relay.contractAddress))
            relay.sendToMaster(etherAddress).send()
            Assertions.assertEquals(BigInteger.valueOf(100_000), cth.getETHBalance(master.contractAddress))
            Assertions.assertEquals(BigInteger.valueOf(0), cth.getETHBalance(relay.contractAddress))
        }
    }

    /**
     * @given deployed master, relay and token contracts, addToken with token contract address is called,
     * 987654 tokens is sent to relay
     * @when sendToMaster of relay contract for token address is called
     * @then relay contract has 0 tokens, master contract has 987654 tokens
     */
    @Test
    fun vacuumTokenTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            master.addToken(token.contractAddress).send()
            token.transfer(relay.contractAddress, BigInteger.valueOf(987_654)).send()
            Assertions.assertEquals(BigInteger.valueOf(0), token.balanceOf(master.contractAddress).send())
            Assertions.assertEquals(BigInteger.valueOf(987_654), token.balanceOf(relay.contractAddress).send())
            relay.sendToMaster(token.contractAddress).send()
            Assertions.assertEquals(BigInteger.valueOf(987_654), token.balanceOf(master.contractAddress).send())
            Assertions.assertEquals(BigInteger.valueOf(0), token.balanceOf(relay.contractAddress).send())
        }
    }

    /**
     * @given deployed master, relay and token contracts, addToken never called,
     * 987654 tokens is sent to relay
     * @when sendToMaster of relay contract for token address is called
     * @then sendToMaster call fails
     */
    @Test
    fun vacuumInvalidTokenTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            token.transfer(relay.contractAddress, BigInteger.valueOf(987_654)).send()
            Assertions.assertEquals(BigInteger.valueOf(0), token.balanceOf(master.contractAddress).send())
            Assertions.assertEquals(BigInteger.valueOf(987_654), token.balanceOf(relay.contractAddress).send())
            Assertions.assertThrows(TransactionException::class.java) {
                relay.sendToMaster(token.contractAddress).send()
            }
        }
    }
}
