package integration

import contract.BasicCoin
import contract.Master
import contract.Relay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.crypto.Hash
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Numeric.hexStringToByteArray
import util.eth.DeployHelper
import util.eth.hashToWithdraw
import util.eth.signUserData
import java.math.BigInteger

/**
 * Class for Ethereum sidechain infrastructure deployment and communication.
 */
class ContractsTest {
    val deploy_helper = DeployHelper()

    private lateinit var token: BasicCoin
    private lateinit var master: Master
    private lateinit var relay: Relay

    // predefined accounts which already exists on parity node
    private val acc_green = "0x00Bd138aBD70e2F00903268F3Db08f2D25677C9e"
    private val acc_another = "0x00aa39d30f0d20ff03a22ccfc30b7efbfca597c2"

    private fun sendAddPeer(address: String) {
        val add_peer = master.addPeer(address).send()
        // addPeer call produces 2 events
        // first event is amount of peers after new peer was added
        // second one is address of added peer
        // events should be aligned to 64 hex digits and prefixed with 0x
        assert(add_peer.logs.size == 2)
        assert(add_peer.logs[0].data == "0x" + String.format("%064x", 1))
        assert(add_peer.logs[1].data == "0x" + "0".repeat(24) +
                address.slice(2 until address.length))
    }

    private fun transferTokensToMaster(amount: BigInteger) {
        token.transfer(master.contractAddress, amount).send()
        assert(token.balanceOf(master.contractAddress).send() == amount)
    }

    private fun withdraw(
            amount: BigInteger,
            iroha_hash: String = Hash.sha3(String.format("%064x", BigInteger.valueOf(12345))),
            token_address: String = token.contractAddress,
            to: String = acc_green,
            from_master: Boolean = true) {
        val final_hash = hashToWithdraw(token_address, amount, to, iroha_hash)

        val signature = signUserData(final_hash)
        val r = hexStringToByteArray(signature.substring(2, 66))
        val s = hexStringToByteArray(signature.substring(66, 130))
        val v = signature.substring(130, 132).toBigInteger(16)

        val vv = ArrayList<BigInteger>()
        vv.add(v)
        val rr = ArrayList<ByteArray>()
        rr.add(r)
        val ss = ArrayList<ByteArray>()
        ss.add(s)

        val byte_hash = hexStringToByteArray(iroha_hash.slice(2 until iroha_hash.length))
        if (from_master) {
            master.withdraw(
                    token_address,
                    amount,
                    to,
                    byte_hash,
                    vv,
                    rr,
                    ss).send()
        } else {
            relay.withdraw(
                    token_address,
                    amount,
                    to,
                    byte_hash,
                    vv,
                    rr,
                    ss).send()
        }
    }

    @BeforeEach
    fun setup() {
        token = deploy_helper.deployBasicCoinSmartContract()
        master = deploy_helper.deployMasterSmartContract()
        relay = deploy_helper.deployRelaySmartContract(master.contractAddress, listOf(token.contractAddress))
    }

    /**
     * @given master account deployed
     * @when transfer 300_000_000 WEIs to master account
     * @then balance of master account increased by 300_000_000
     */
    @Test
    fun canAcceptEther() {
        val initial_balance = deploy_helper.web3.ethGetBalance(master.contractAddress, DefaultBlockParameterName.LATEST).send().balance
        deploy_helper.sendEthereum(BigInteger.valueOf(300_000_000), master.contractAddress)
        // have to wait some time until balance will be updated
        Thread.sleep(15000)
        assert(initial_balance + BigInteger.valueOf(300_000_000) ==
                deploy_helper.web3.ethGetBalance(master.contractAddress, DefaultBlockParameterName.LATEST).send().balance)
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master
     * @then call to withdraw succeeded
     */
    @Test
    fun singleCorrectSignatureTokenTest() {
        sendAddPeer(deploy_helper.credentials.address)
        transferTokensToMaster(BigInteger.valueOf(5))
        withdraw(BigInteger.valueOf(1))
        assert(token.balanceOf(master.contractAddress).send() == BigInteger.valueOf(4))
    }

    /**
     * @given deployed master contract
     * @when one peer added to master, 5000 Wei transferred to master,
     * request to withdraw 1000 Wei is sent to master
     * @then call to withdraw succeeded
     */
    @Test
    fun singleCorrectSignatureEtherTestMaster() {
        val initial_balance = deploy_helper.web3.ethGetBalance(acc_another, DefaultBlockParameterName.LATEST).send().balance
        sendAddPeer(deploy_helper.credentials.address)
        deploy_helper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
        // have to wait some time until balance will be updated
        Thread.sleep(15000)
        withdraw(BigInteger.valueOf(1000), token_address = "0x0000000000000000000000000000000000000000", to = acc_another)
        assertEquals(BigInteger.valueOf(4000),
                deploy_helper.web3.ethGetBalance(master.contractAddress, DefaultBlockParameterName.LATEST).send().balance)
        assertEquals(initial_balance + BigInteger.valueOf(1000),
                deploy_helper.web3.ethGetBalance(acc_another, DefaultBlockParameterName.LATEST).send().balance)
    }

    /**
     * @given deployed master and relay contracts
     * @when one peer added to master, 5000 Wei transferred to master,
     * request to withdraw 1000 Wei is sent to relay
     * @then call to withdraw succeeded
     */
    @Test
    fun singleCorrectSignatureEtherTestRelay() {
        val initial_balance = deploy_helper.web3.ethGetBalance(acc_another, DefaultBlockParameterName.LATEST).send().balance
        sendAddPeer(deploy_helper.credentials.address)
        deploy_helper.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
        // have to wait some time until balance will be updated
        Thread.sleep(15000)
        withdraw(BigInteger.valueOf(1000), token_address = "0x0000000000000000000000000000000000000000",
                from_master = false, to = acc_another)
        assertEquals(BigInteger.valueOf(4000),
                deploy_helper.web3.ethGetBalance(master.contractAddress, DefaultBlockParameterName.LATEST).send().balance)
        assertEquals(initial_balance + BigInteger.valueOf(1000),
                deploy_helper.web3.ethGetBalance(acc_another, DefaultBlockParameterName.LATEST).send().balance)
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master twice
     * @then second call to withdraw failed
     */
    @Test
    fun usedHashTest() {
        sendAddPeer(deploy_helper.credentials.address)
        transferTokensToMaster(BigInteger.valueOf(5))
        withdraw(BigInteger.valueOf(1))
        assert(token.balanceOf(master.contractAddress).send() == BigInteger.valueOf(4))
        withdraw(BigInteger.valueOf(1))
        assert(token.balanceOf(master.contractAddress).send() == BigInteger.valueOf(4))
    }
}
