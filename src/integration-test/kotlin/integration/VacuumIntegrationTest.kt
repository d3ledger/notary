package integration

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import com.google.protobuf.InvalidProtocolBufferException
import config.EthereumPasswords
import config.TestConfig
import config.loadConfigs
import io.grpc.ManagedChannelBuilder
import iroha.protocol.Queries
import iroha.protocol.QueryServiceGrpc
import jp.co.soramitsu.iroha.ModelProtoQuery
import jp.co.soramitsu.iroha.ModelQueryBuilder
import kotlinx.coroutines.experimental.async
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sidechain.eth.util.DeployHelper
import sidechain.iroha.IrohaInitialization
import sidechain.iroha.consumer.IrohaNetworkImpl
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.getRelays
import sidechain.iroha.util.toByteArray
import java.math.BigInteger

/**
 * Integration tests for vacuum usecase.
 */
class VacuumIntegrationTest {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure {
                println(it)
                System.exit(1)
            }
    }

    /** Configurations for tests */
    private val testConfig = loadConfigs("test", TestConfig::class.java)

    /** Ethereum password configs */
    private val passwordConfig = loadConfigs("test", EthereumPasswords::class.java, "/ethereum_password.properties")

    /** Ethereum utils */
    private val deployHelper = DeployHelper(testConfig.ethereum, passwordConfig)

    /** Notary account in Iroha */
    val notaryAccount = testConfig.notaryIrohaAccount

    /** Iroha keypair */
    val keypair = ModelUtil.loadKeypair(testConfig.iroha.pubkeyPath, testConfig.iroha.privkeyPath).get()

    /** Iroha network */
    val irohaNetwork = IrohaNetworkImpl(testConfig.iroha.hostname, testConfig.iroha.port)

    /** Iroha transaction creator */
    val creator = testConfig.iroha.creator


    @Test
    fun testVacuum() {
        val masterAccount = testConfig.notaryIrohaAccount
        val assetId = "ether#ethereum"
        async {
            registration.relay.main(emptyArray())
        }
        val amount = BigInteger.valueOf(1_234_000_000_000)
        var totalAmount = BigInteger.ZERO
        Thread.sleep(30_000)
        getFreeWallets().get().forEach { ethPublicKey ->
            deployHelper.sendEthereum(amount, ethPublicKey)
            totalAmount = totalAmount.add(amount)
        }
        Thread.sleep(120_000)
        async {
            vacuum.main(emptyArray())
        }
        Thread.sleep(120_000)
        Assertions.assertEquals(totalAmount, getBalance(assetId, masterAccount))

    }

    fun getBalance(assetId: String, accountId: String): BigInteger {
        val queryCounter: Long = 1

        val uquery = ModelQueryBuilder()
            //TODO wtf???
            .creatorAccountId(creator)
            .queryCounter(BigInteger.valueOf(queryCounter))
            .createdTime(BigInteger.valueOf(System.currentTimeMillis()))
            .getAccountAssets(accountId)
            .build()
        val queryBlob = ModelProtoQuery(uquery).signAndAddSignature(keypair).finish().blob().toByteArray()

        val protoQuery: Queries.Query?
        try {
            protoQuery = Queries.Query.parseFrom(queryBlob)
        } catch (e: InvalidProtocolBufferException) {
            fail { "Exception while converting byte array to protobuf:" + e.message }
        }

        val channel =
            ManagedChannelBuilder.forAddress(testConfig.iroha.hostname, testConfig.iroha.port).usePlaintext(true)
                .build()
        val queryStub = QueryServiceGrpc.newBlockingStub(channel)
        val queryResponse = queryStub.find(protoQuery)

        val fieldDescriptor = queryResponse.descriptorForType.findFieldByName("account_assets_response")
        if (!queryResponse.hasField(fieldDescriptor)) {
            fail { "Query response error ${queryResponse.errorResponse}" }
        }

        val assets = queryResponse.accountAssetsResponse.accountAssetsList
        for (asset in assets) {
            if (assetId == asset.assetId)
                return BigInteger(asset.balance)
        }

        return BigInteger.ZERO
    }

    fun getFreeWallets(): Result<Set<String>, Exception> {
        return getRelays(testConfig.iroha, keypair, irohaNetwork, notaryAccount, testConfig.registrationIrohaAccount)
            .map { keyMapping ->
                val freeWallets = keyMapping.filterValues { irohaKey -> irohaKey == "free" }.keys
                if (freeWallets.isEmpty())
                    throw Exception("EthFreeWalletsProvider - no free relay wallets created by $testConfig.registrationIrohaAccount")
                else
                    freeWallets
            }
    }

}