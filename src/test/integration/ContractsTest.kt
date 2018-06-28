package integration

import contract.BasicCoin
import contract.Master
import contract.Relay
import main.CONFIG
import main.ConfigKeys
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Hash
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.parity.Parity
import java.math.BigInteger

/**
 * Class for Ethereum sidechain infrastructure deployment and communication.
 */
class ContractsTest {
    val deploy_helper = DeployHelper()

    private lateinit var token: BasicCoin
    private lateinit var master: Master
    private lateinit var relay: Relay

    // predefined account which already exists on parity node
    private val acc_green = "0x00Bd138aBD70e2F00903268F3Db08f2D25677C9e"

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun signUserData(to_sign: String): String {
        // TODO luckychess 26.06.2018 D3-100 find a way to produce correct signatures locally
        val parity = Parity.build(HttpService(CONFIG[ConfigKeys.ethConnectionUrl]))
        val unlock = parity.personalUnlockAccount(deploy_helper.credentials.address, "user").send()
        assert(unlock.accountUnlocked())
        val signature = parity.ethSign(deploy_helper.credentials.address, to_sign).send().signature
        println("eth_sign: $signature")
        return signature
    }

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
            iroha_hash: String = Hash.sha3(String.format("%064x", BigInteger.valueOf(12345)))) {
        // it was really painful to find out how to combine fields
        // in a same way as js and solidity do
        val final_hash = Hash.sha3(token.contractAddress.replace("0x", "")
                + String.format("%064x", amount).replace("0x", "")
                + acc_green.replace("0x", "")
                + iroha_hash.replace("0x", ""))
        println("hash: $final_hash")

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
        master.withdraw(
                token.contractAddress,
                amount,
                acc_green,
                byte_hash,
                vv,
                rr,
                ss).send()
    }

    @BeforeEach
    fun setup() {
        token = deploy_helper.deployBasicCoinSmartContract()
        master = deploy_helper.deployMasterSmartContract()
        relay = deploy_helper.deployRelaySmartContract(master.contractAddress, listOf(token.contractAddress))
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master
     * @then call to withdraw succeeded
     */
    @Test
    fun singleCorrectSignatureTest() {
        sendAddPeer(deploy_helper.credentials.address)
        transferTokensToMaster(BigInteger.valueOf(5))
        withdraw(BigInteger.valueOf(1))
        assert(token.balanceOf(master.contractAddress).send() == BigInteger.valueOf(4))
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
