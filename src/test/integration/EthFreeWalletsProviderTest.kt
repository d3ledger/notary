package integration

import com.github.kittinunf.result.failure
import com.google.protobuf.InvalidProtocolBufferException
import config.loadConfigs
import io.grpc.ManagedChannelBuilder
import iroha.protocol.BlockOuterClass
import iroha.protocol.CommandServiceGrpc
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelProtoTransaction
import jp.co.soramitsu.iroha.ModelTransactionBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import registration.EthFreeWalletsProvider
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.toByteArray
import java.math.BigInteger

class EthFreeWalletsProviderTest {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure {
                println(it)
                System.exit(1)
            }
    }

    /** Test configurations */
    val testConfig = loadConfigs("test", TestConfig::class.java)

    /** Iroha keypair */
    val keypair: Keypair =
        ModelUtil.loadKeypair(
            testConfig.iroha.pubkeyPath,
            testConfig.iroha.privkeyPath
        ).get()

    /** Iroha host */
    val irohaHost = testConfig.iroha.hostname

    /** Iroha port */
    val irohaPort = testConfig.iroha.port

    /** Iroha transaction creator */
    val creator = testConfig.iroha.creator

    /** Iroha master */
    val relayRegistrationIrohaAccount = testConfig.relayRegistrationIrohaAccount

    /**
     * Send SetAccountDetail to Iroha
     * @return hex representation of transaction hash
     */
    fun setAccountDetail(accountId: String, key: String, value: String): String {
        val currentTime = System.currentTimeMillis()

        // build transaction (still unsigned)
        val txBuilder = ModelTransactionBuilder().creatorAccountId(creator)
            .createdTime(BigInteger.valueOf(currentTime))
            .setAccountDetail(accountId, key, value)
        return sendTxToIroha(txBuilder)
    }

    /**
     * Sends transaction to Iroha.
     * @return hex representation of transaction
     */
    fun sendTxToIroha(txBuilder: ModelTransactionBuilder): String {
        val utx = txBuilder.build()
        val hash = utx.hash().hex()

        // sign transaction and get its binary representation (Blob)
        val txblob = ModelProtoTransaction(utx).signAndAddSignature(keypair).finish().blob().toByteArray()

        // create proto object
        var protoTx: BlockOuterClass.Transaction? = null
        try {
            protoTx = BlockOuterClass.Transaction.parseFrom(txblob)
        } catch (e: InvalidProtocolBufferException) {
            System.err.println("Exception while converting byte array to protobuf:" + e.message)
            System.exit(1)
        }

        // Send transaction to iroha
        val channel = ManagedChannelBuilder.forAddress(irohaHost, irohaPort).usePlaintext(true).build()
        val stub = CommandServiceGrpc.newBlockingStub(channel)
        stub.torii(protoTx)

        return hash
    }

    /**
     * @given Iroha network running and Iroha master account with attribute ["eth_wallet", "free"] set by master account
     * @when getWallet() of FreeWalletProvider is called
     * @then "eth_wallet" attribute key is returned
     */
    @Test
    fun getFreeWallet() {
        val ethFreeWallet = "eth_free_wallet_stub"

        setAccountDetail(relayRegistrationIrohaAccount, ethFreeWallet, "free")
        Thread.sleep(4_000)

        val freeWalletsProvider =
            EthFreeWalletsProvider(testConfig.iroha, keypair, creator, relayRegistrationIrohaAccount)
        val result = freeWalletsProvider.getWallet()

        assertEquals(ethFreeWallet, result)
    }

    /**
     * @given Iroha network running and Iroha master account
     * @when getWallet() of FreeWalletProvider is called with wrong master account
     * @then "eth_wallet" attribute key is returned
     */
    @Test
    fun getFreeWalletException() {
        val ethFreeWallet = "eth_free_wallet_stub"
        val wrongMasterAccount = "wrong@account"

        setAccountDetail(relayRegistrationIrohaAccount, ethFreeWallet, "free")
        Thread.sleep(4_000)

        val freeWalletsProvider = EthFreeWalletsProvider(testConfig.iroha, keypair, creator, wrongMasterAccount)
        assertThrows<Exception> { freeWalletsProvider.getWallet() }
    }
}
