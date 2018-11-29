package integration.eth

import integration.helper.IntegrationHelperUtil
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelCrypto
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import org.junit.jupiter.api.*
import provider.eth.ETH_PRECISION
import sidechain.iroha.CLIENT_DOMAIN
import util.getRandomString
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Integration tests for withdrawal rollback service.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WithdrawalRollbackIntegrationTest {

    /** Integration tests util */
    private val integrationHelper = IntegrationHelperUtil()

    /** Test Registration configuration */
    private val registrationConfig = integrationHelper.ethRegistrationConfig

    /** Ethereum test address where we want to withdraw to */
    private val toAddress = integrationHelper.configHelper.testConfig.ethTestAccount

    /** Notary account in Iroha */
    private val notaryAccount = integrationHelper.accountHelper.notaryAccount.accountId

    private val txStorage = integrationHelper.getTestingWithdrawalTxStorage()

    private val registrationService: Job

    private val withdrawalService: Job

    private val wdRollbackService: Job

    init {
        integrationHelper.runEthNotary()
        registrationService = launch {
            integrationHelper.runRegistrationService(registrationConfig)
        }
        withdrawalService = launch {
            integrationHelper.runEthWithdrawalService()
        }
        wdRollbackService = launch {
            integrationHelper.runWdRollbackService()
        }

        integrationHelper.lockEthMasterSmartcontract()
    }

    lateinit var clientName: String
    lateinit var clientId: String
    lateinit var keypair: Keypair

    @BeforeEach
    fun setup() {
        // generate client name and key
        clientName = String.getRandomString(9)
        clientId = "$clientName@$CLIENT_DOMAIN"
        keypair = ModelCrypto().generateKeypair()
    }

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
        registrationService.cancel()
        withdrawalService.cancel()
        wdRollbackService.cancel()
    }

    /**
     * Full withdrawal pipeline test
     * @given iroha, withdrawal and rollback services are running, free relays available, user account has 250 Wei in Iroha
     * @when user transfers 250 Wei to Iroha master account and that withdrawal crashes
     * @then balance of user's wallet in Iroha becomes the initial one
     */
    @Test
    fun testFullWithdrawalRollbackPipeline() {
        val amount = BigInteger.valueOf(2502800000000)

        // deploy free relay
        integrationHelper.deployRelays(1)

        // make sure master has enough assets
        integrationHelper.sendEth(amount, integrationHelper.masterContract.contractAddress)

        // register client
        val res = integrationHelper.sendRegistrationRequest(
            clientName,
            listOf(toAddress).toString(),
            keypair.publicKey(),
            registrationConfig.port
        )
        Assertions.assertEquals(200, res.statusCode)

        val decimalAmount = BigDecimal(amount, ETH_PRECISION.toInt()).toPlainString()
        val assetId = "ether#ethereum"

        // add assets to user
        integrationHelper.addIrohaAssetTo(clientId, assetId, decimalAmount)

        val initialBalance = integrationHelper.getIrohaAccountBalance(clientId, assetId)

        // transfer assets from user to notary master account
        val txHash = integrationHelper.transferAssetIrohaFromClient(
            clientId,
            keypair,
            clientId,
            notaryAccount,
            assetId,
            toAddress,
            decimalAmount
        )
        Thread.sleep(10_000)

        // simulate withdrawal failure
        txStorage.put(txHash, "null")

        Thread.sleep(10_000)

        Assertions.assertEquals(
            initialBalance,
            integrationHelper.getIrohaAccountBalance(clientId, assetId)
        )
    }
}
