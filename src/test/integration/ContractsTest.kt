package integration

import contract.BasicCoin
import contract.Master
import contract.Relay
import main.CONFIG
import main.ConfigKeys
import org.junit.jupiter.api.BeforeAll
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
    companion object {
        val deploy_helper = DeployHelper()

        private lateinit var token: BasicCoin
        private lateinit var master: Master
        private lateinit var relay: Relay

        @BeforeAll
        @JvmStatic
        fun setup() {
            token = deploy_helper.deployBasicCoinSmartContract()
            master = deploy_helper.deployMasterSmartContract()
            relay = deploy_helper.deployRelaySmartContract(master.contractAddress, listOf(token.contractAddress))
        }

        fun hexStringToByteArray(s: String): ByteArray {
            val len = s.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
                i += 2
            }
            return data
        }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master
     * @then call to withdraw succeeded
     */
    @Test
    fun masterTest() {
        val add_peer = master.addPeer(deploy_helper.credentials.address).send()
        // addPeer call produces 2 events
        // first event is amount of peers after new peer was added
        // second one is address of added peer
        // events should be aligned to 64 hex digits and prefixed with 0x
        assert(add_peer.logs.size == 2)
        assert(add_peer.logs[0].data == "0x" + String.format("%064x", 1))
        assert(add_peer.logs[1].data == "0x" + "0".repeat(24) +
                deploy_helper.credentials.address.slice(2 until deploy_helper.credentials.address.length))

        token.transfer(master.contractAddress, BigInteger.valueOf(5)).send()
        assert(token.balanceOf(master.contractAddress).send() == BigInteger.valueOf(5))

        val num = Uint256(BigInteger.valueOf(1))
        // predefined account which already exists on parity node
        val acc_green = "0x00Bd138aBD70e2F00903268F3Db08f2D25677C9e"
        // this is intended to be a hash of iroha tx
        // let's just hash some random data
        val random_hash = Hash.sha3(String.format("%064x", BigInteger.valueOf(12345)))

        // it was really painful to find out how to combine fields
        // in a same way as js and solidity do
        val final_hash = Hash.sha3(token.contractAddress.replace("0x", "")
                + String.format("%064x", num.value).replace("0x", "")
                + acc_green.replace("0x", "")
                + random_hash.replace("0x", ""))
        println("hash: $final_hash")

        // TODO luckychess 26.06.2018 D3-100 find a way to produce correct signatures locally
        val parity = Parity.build(HttpService(CONFIG[ConfigKeys.ethConnectionUrl]))
        val unlock = parity.personalUnlockAccount(deploy_helper.credentials.address, "user").send()
        assert(unlock.accountUnlocked())
        val signature = parity.ethSign(deploy_helper.credentials.address, final_hash).send().signature
        println("eth_sign: $signature")

        val r = hexStringToByteArray(signature.substring(2, 66))
        val s = hexStringToByteArray(signature.substring(66, 130))
        val v = signature.substring(130, 132).toBigInteger(16)

        val vv = ArrayList<BigInteger>()
        vv.add(v)
        val rr = ArrayList<ByteArray>()
        rr.add(r)
        val ss = ArrayList<ByteArray>()
        ss.add(s)

        val byte_hash = hexStringToByteArray(random_hash.slice(2 until random_hash.length))
        master.withdraw(
                token.contractAddress,
                num.value,
                acc_green,
                byte_hash,
                vv,
                rr,
                ss).send()

        assert(token.balanceOf(master.contractAddress).send() == BigInteger.valueOf(4))
    }
}
