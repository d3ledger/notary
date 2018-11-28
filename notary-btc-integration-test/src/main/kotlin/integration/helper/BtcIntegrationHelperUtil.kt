package integration.helper

import com.github.jleskovar.btcrpc.BitcoinRpcClientFactory
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelCrypto
import mu.KLogging
import notary.IrohaCommand
import notary.IrohaOrderedBatch
import notary.IrohaTransaction
import org.bitcoinj.core.Address
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.wallet.Wallet
import provider.btc.account.IrohaBtcAccountCreator
import provider.btc.address.AddressInfo
import provider.btc.address.BtcAddressesProvider
import provider.btc.address.BtcRegisteredAddressesProvider
import registration.btc.BtcRegistrationStrategyImpl
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.util.ModelUtil
import java.io.File
import java.math.BigDecimal

const val btcAsset = "btc#bitcoin"

class BtcIntegrationHelperUtil : IrohaIntegrationHelperUtil() {

    override val configHelper by lazy { BtcConfigHelper(accountHelper) }

    private val mstRegistrationIrohaConsumer by lazy {
        IrohaConsumerImpl(accountHelper.mstRegistrationAccount, irohaNetwork)
    }

    private val rpcClient by lazy {
        BitcoinRpcClientFactory.createClient(
            user = "test",
            password = "test",
            host = configHelper.createBtcNotaryConfig().bitcoin.host,
            port = 8332,
            secure = false
        )
    }

    private val btcRegistrationStrategy by lazy {
        val btcAddressesProvider =
            BtcAddressesProvider(
                testCredential,
                irohaNetwork,
                accountHelper.mstRegistrationAccount.accountId,
                accountHelper.notaryAccount.accountId
            )
        val btcTakenAddressesProvider =
            BtcRegisteredAddressesProvider(
                testCredential,
                irohaNetwork,
                accountHelper.registrationAccount.accountId,
                accountHelper.notaryAccount.accountId
            )
        val irohaBtcAccountCreator = IrohaBtcAccountCreator(
            registrationConsumer,
            accountHelper.notaryAccount.accountId
        )
        BtcRegistrationStrategyImpl(
            btcAddressesProvider,
            btcTakenAddressesProvider,
            irohaBtcAccountCreator
        )
    }

    /**
     * Pregenerates multiple BTC address that can be registered later
     * @param walletFilePath - path to wallet file
     * @param addressesToGenerate - number of addresses to generate
     * @return result of operation
     */
    fun preGenFreeBtcAddresses(walletFilePath: String, addressesToGenerate: Int): Result<Unit, Exception> {
        return Result.of {
            val irohaTxList = ArrayList<IrohaTransaction>()
            for (i in 1..addressesToGenerate) {
                val (key, address) = generateKeyAndAddress(walletFilePath)
                val irohaTx = IrohaTransaction(
                    mstRegistrationIrohaConsumer.creator,
                    ModelUtil.getCurrentTime(),
                    1,
                    arrayListOf(
                        IrohaCommand.CommandSetAccountDetail(
                            accountHelper.notaryAccount.accountId,
                            address.toBase58(),
                            AddressInfo.createFreeAddressInfo(listOf(key.publicKeyAsHex)).toJson()
                        )
                    )
                )
                irohaTxList.add(irohaTx)
            }
            val utx = IrohaConverterImpl().convert(IrohaOrderedBatch(irohaTxList))
            mstRegistrationIrohaConsumer.sendAndCheck(utx).failure { ex -> throw ex }
        }
    }

    private fun createMsAddress(keys: List<ECKey>): Address {
        val script = ScriptBuilder.createP2SHOutputScript(1, keys)
        return script.getToAddress(RegTestParams.get())
    }

    /**
     * Generates one BTC address that can be registered later
     * @param walletFilePath - path to wallet file
     * @return randomly generated BTC address
     */
    fun genFreeBtcAddress(walletFilePath: String): Result<Address, Exception> {
        val (key, address) = generateKeyAndAddress(walletFilePath)
        return ModelUtil.setAccountDetail(
            mstRegistrationIrohaConsumer,
            accountHelper.notaryAccount.accountId,
            address.toBase58(),
            AddressInfo.createFreeAddressInfo(listOf(key.publicKeyAsHex)).toJson()
        ).map { address }
    }

    /**
     * Generates one BTC address that can be registered later
     * @param walletFilePath - path to wallet file
     * @return randomly generated BTC address
     */
    fun genChangeBtcAddress(walletFilePath: String): Result<Address, Exception> {
        val (key, address) = generateKeyAndAddress(walletFilePath)
        return ModelUtil.setAccountDetail(
            mstRegistrationIrohaConsumer,
            accountHelper.changeAddressesStorageAccount.accountId,
            address.toBase58(),
            AddressInfo.createChangeAddressInfo(listOf(key.publicKeyAsHex)).toJson()
        ).map { address }
    }

    // Generates key and key based address
    private fun generateKeyAndAddress(walletFilePath: String): Pair<DeterministicKey, Address> {
        val walletFile = File(walletFilePath)
        val wallet = Wallet.loadFromFile(walletFile)
        val key = wallet.freshReceiveKey()
        val address = createMsAddress(listOf(key))
        wallet.addWatchedAddress(address)
        wallet.saveToFile(walletFile)
        logger.info { "generated address $address" }
        return Pair(key, address)
    }

    /**
     * Registers BTC client
     * @param walletFilePath - path to wallet file
     * @param irohaAccountName - client account in Iroha
     * @param keypair - key pair for new client in Iroha
     * @return btc address related to client
     */
    fun registerBtcAddress(
        walletFilePath: String,
        irohaAccountName: String,
        keypair: Keypair = ModelCrypto().generateKeypair()
    ): String {
        genFreeBtcAddress(walletFilePath).fold({
            return registerBtcAddressNoPreGen(irohaAccountName, keypair)
        }, { ex -> throw ex })
    }

    /**
     * Registers BTC client with no generation
     * @param irohaAccountName - client account in Iroha
     * @param keypair - key pair of new client in Iroha
     * @param whitelist - list available addresses to send money to
     * @return btc address related to client
     */
    fun registerBtcAddressNoPreGen(
        irohaAccountName: String,
        keypair: Keypair = ModelCrypto().generateKeypair(),
        whitelist: List<String> = emptyList()
    ): String {
        btcRegistrationStrategy.register(irohaAccountName, whitelist, keypair.publicKey().hex())
            .fold({ btcAddress ->
                logger.info { "BTC address $btcAddress was registered by $irohaAccountName" }
                return btcAddress
            }, { ex -> throw ex })
    }

    /**
     * Sends btc to a given address
     */
    fun sendBtc(address: String, amount: Int, confirmations: Int = 6) {
        rpcClient.sendToAddress(address = address, amount = BigDecimal(amount))
        generateBtcBlocks(confirmations)
    }

    /**
     * Creates blocks in bitcoin blockchain. May be used as transaction confirmation mechanism.
     */
    fun generateBtcBlocks(blocks: Int = 150) {
        if (blocks > 0) {
            rpcClient.generate(numberOfBlocks = blocks)
            logger.info { "New $blocks ${singularOrPluralBlocks(blocks)} generated in Bitcoin blockchain" }
        }
    }

    private fun singularOrPluralBlocks(blocks: Int): String {
        if (blocks == 1) {
            return "block was"
        }
        return "blocks were"
    }

    // Converts Bitcoins to Satoshi
    fun btcToSat(btc: Int): Long {
        return btc * 100_000_000L
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
