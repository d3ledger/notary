/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.eth

import com.d3.commons.sidechain.iroha.CLIENT_DOMAIN
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import com.d3.eth.provider.ETH_PRECISION
import integration.helper.EthIntegrationHelperUtil
import integration.registration.RegistrationServiceTestEnvironment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.jupiter.api.*
import java.math.BigDecimal
import java.math.BigInteger
import java.security.KeyPair

/**
 * Integration tests for withdrawal rollback service.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WithdrawalRollbackIntegrationTest {

    /** Integration tests util */
    private val integrationHelper = EthIntegrationHelperUtil()

    private val registrationTestEnvironment = RegistrationServiceTestEnvironment(integrationHelper)

    /** Test Registration configuration */
    private val registrationConfig = registrationTestEnvironment.registrationConfig

    /** Test EthRegistration configuration */
    private val ethRegistrationConfig = integrationHelper.ethRegistrationConfig

    /** Ethereum test address where we want to withdraw to */
    private val toAddress = integrationHelper.configHelper.testConfig.ethTestAccount

    /** Notary account in Iroha */
    private val notaryAccount = integrationHelper.accountHelper.notaryAccount.accountId

    private val ethRegistrationService: Job

    private val withdrawalService: Job

    init {
        integrationHelper.runEthDeposit()
        registrationTestEnvironment.registrationInitialization.init()
        ethRegistrationService = GlobalScope.launch {
            integrationHelper.runEthRegistrationService(ethRegistrationConfig)
        }
        withdrawalService = GlobalScope.launch {
            integrationHelper.runEthWithdrawalService(
                integrationHelper.configHelper.createWithdrawalConfig(
                    String.getRandomString(9), false
                )
            )
        }
    }

    lateinit var clientName: String
    lateinit var clientId: String
    lateinit var keypair: KeyPair

    @BeforeEach
    fun setup() {
        // generate client name and key
        clientName = String.getRandomString(9)
        clientId = "$clientName@$CLIENT_DOMAIN"
        keypair = ModelUtil.generateKeypair()
    }

    @AfterAll
    fun dropDown() {
        registrationTestEnvironment.close()
        ethRegistrationService.cancel()
        withdrawalService.cancel()
        integrationHelper.close()
    }

    /**
     * Full withdrawal rollback pipeline test
     * @given iroha and withdrawal services are running, free relays available, user account has 250 Wei in Iroha
     * @when user transfers 250 Wei to Iroha master account and that withdrawal crashes
     * @then balance of user's wallet in Iroha becomes the initial one
     */
    @Test
    fun testWithdrawalRollbackPipeline() {
        val amount = BigInteger.valueOf(2502800000000)

        // deploy free relay
        integrationHelper.deployRelays(1)

        // make sure master has enough assets
        integrationHelper.sendEth(amount, integrationHelper.masterContract.contractAddress)

        // register client in Iroha
        var res = integrationHelper.sendRegistrationRequest(
            clientName,
            listOf(toAddress).toString(),
            keypair.public.toHexString(),
            registrationConfig.port
        )
        Assertions.assertEquals(200, res.statusCode)

        // register client in Ethereum
        res = integrationHelper.sendRegistrationRequest(
            clientName,
            listOf(toAddress).toString(),
            keypair.public.toHexString(),
            ethRegistrationConfig.port
        )
        Assertions.assertEquals(200, res.statusCode)

        val decimalAmount = BigDecimal(amount, ETH_PRECISION)
        val assetId = "ether#ethereum"

        // add assets to user
        integrationHelper.addIrohaAssetTo(clientId, assetId, decimalAmount)

        val initialBalance = integrationHelper.getIrohaAccountBalance(clientId, assetId)

        // transfer assets from user to notary master account
        integrationHelper.transferAssetIrohaFromClient(
            clientId,
            keypair,
            clientId,
            notaryAccount,
            assetId,
            toAddress,
            decimalAmount.toPlainString()
        )

        Thread.sleep(35_000)

        Assertions.assertEquals(
            initialBalance,
            integrationHelper.getIrohaAccountBalance(clientId, assetId)
        )
    }
}
