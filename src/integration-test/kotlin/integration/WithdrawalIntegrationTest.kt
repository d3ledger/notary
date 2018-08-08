package integration

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.EthereumPasswords
import config.TestConfig
import config.loadConfigs
import contract.BasicCoin
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelCrypto
import jp.co.soramitsu.iroha.ModelTransactionBuilder
import kotlinx.coroutines.experimental.async
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.web3j.protocol.core.DefaultBlockParameterName
import sidechain.eth.util.DeployHelper
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import java.math.BigInteger

/**
 * Integration tests for withdrawal usecase.
 */
class WithdrawalIntegrationTest {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure {
                println(it)
                System.exit(1)
            }
    }

    /** Configurations for tests */
    private val testConfig = loadConfigs("test", TestConfig::class.java)

    /** Iroha transaction creator */
    val creator = testConfig.iroha.creator

    /** Iroha network layer */
    val irohaConsumer = IrohaConsumerImpl(testConfig.iroha)

    /** Ethereum password configs */
    private val passwordConfig = loadConfigs("test", EthereumPasswords::class.java, "/ethereum_password.properties")

    /** Ethereum utils */
    private val deployHelper = DeployHelper(testConfig.ethereum, passwordConfig)

    /** Ethereum test address where we want to withdraw to */
    private val toAddress = testConfig.ropstenTestAccount

    /** Registration service port */
    private val registrationServicePort = 8083

    /** Notary account in Iroha */
    val notaryAccount = testConfig.notaryIrohaAccount

    /**
     * Transfer asset in iroha with custom keypair
     * @param creator - iroha transaction creator
     * @param kp - keypair
     * @param srcAccountId - source account id
     * @param destAccountId - destination account id
     * @param assetId - asset id
     * @param description - transaction description
     * @param amount - amount
     * @return hex representation of transaction hash
     */
    fun transferAssetIroha(
        creator: String,
        kp: Keypair,
        srcAccountId: String,
        destAccountId: String,
        assetId: String,
        description: String,
        amount: String
    ): String {
        val utx = ModelTransactionBuilder()
            .creatorAccountId(creator)
            .createdTime(ModelUtil.getCurrentTime())
            .transferAsset(srcAccountId, destAccountId, assetId, description, amount)
            .build()
        val hash = utx.hash()
        return ModelUtil.prepareTransaction(utx, kp)
            .flatMap { IrohaNetworkImpl(testConfig.iroha.hostname, testConfig.iroha.port).sendAndCheck(it, hash) }
            .get()
    }

    /**
     * Full withdrawal pipeline test
     * @given iroha and withdrawal services are running, free relays available, user account has 125 Wei in Iroha
     * @when user transfers 125 Wei to Iroha master account
     * @then balance of user's wallet in Ethereum increased by 125 Wei
     */
    @Test
    fun testFullWithdrawalPipeline() {
        val name = String.getRandomString(9)
        val fullName = "$name@notary"
        val keypair = ModelCrypto().generateKeypair()

        async {
            registration.main(emptyArray())
        }

        async {
            withdrawalservice.main(emptyArray())
        }
        Thread.sleep(10_000)

        val res = khttp.post(
            "http://127.0.0.1:$registrationServicePort/users",
            data = mapOf("name" to name, "pubkey" to keypair.publicKey().hex())
        )
        Assertions.assertEquals(200, res.statusCode)

        val initialBalance =
            deployHelper.web3.ethGetBalance(toAddress, DefaultBlockParameterName.LATEST).send().balance

        val amount = "125"
        val assetId = "ether#ethereum"

        // add assets to user
        ModelUtil.addAssetIroha(irohaConsumer, creator, assetId, amount)
        println("add asset")
        Thread.sleep(5_000)
        ModelUtil.transferAssetIroha(irohaConsumer, creator, creator, fullName, assetId, "", amount)
        Thread.sleep(5_000)
        println("transfer asset to $fullName")

        // transfer assets from user to notary master account
        transferAssetIroha(fullName, keypair, fullName, notaryAccount, assetId, toAddress, amount)
        println("transfer asset to $notaryAccount")
        Thread.sleep(300_000)

        Assertions.assertEquals(
            initialBalance + BigInteger.valueOf(amount.toLong()),
            deployHelper.web3.ethGetBalance(toAddress, DefaultBlockParameterName.LATEST).send().balance
        )
    }

    /**
     * Full withdrawal pipeline test for ERC20 token
     * @given iroha and withdrawal services are running, free relays available, user account has 125 OMG tokens in Iroha
     * @when user transfers 125 OMG to Iroha master account
     * @then balance of user's wallet in Ethereum increased by 125 OMG
     */
    @Test
    fun testFullWithdrawalPipelineErc20() {
        val name = String.getRandomString(9)
        val fullName = "$name@notary"
        val keypair = ModelCrypto().generateKeypair()

        /** ERC20 token "OMG" address */
        val tokenAddress = "0x9d65d6209bcd37f1f546315171b000663117d42f"

        // run services
        async {
            registration.main(emptyArray())
        }

        async {
            withdrawalservice.main(emptyArray())
        }
        Thread.sleep(10_000)

        // register user
        val res = khttp.post(
            "http://127.0.0.1:$registrationServicePort/users",
            data = mapOf("name" to name, "pubkey" to keypair.publicKey().hex())
        )
        Assertions.assertEquals(200, res.statusCode)

        val token = BasicCoin.load(
            tokenAddress,
            deployHelper.web3,
            deployHelper.credentials,
            deployHelper.gasPrice,
            deployHelper.gasLimit
        )

        val initialBalance = token.balanceOf(toAddress).send()

        val amount = "125"
        val assetName = "omg"
        val domain = "ethereum"
        val assetId = "$assetName#$domain"

        ModelUtil.createAsset(irohaConsumer, creator, assetName, domain, 0)

        // add assets to user
        ModelUtil.addAssetIroha(irohaConsumer, creator, assetId, amount)
        Thread.sleep(5_000)
        ModelUtil.transferAssetIroha(irohaConsumer, creator, creator, fullName, assetId, "", amount)
        Thread.sleep(5_000)

        // transfer assets from user to notary master account
        transferAssetIroha(fullName, keypair, fullName, notaryAccount, assetId, toAddress, amount)
        Thread.sleep(300_000)

        Assertions.assertEquals(initialBalance + BigInteger.valueOf(amount.toLong()), token.balanceOf(toAddress).send())
    }
}
